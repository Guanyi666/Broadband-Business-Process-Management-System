package com.bbpms.order.service.impl;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.CryptoUtils;
import com.bbpms.order.config.OrderProperties;
import com.bbpms.order.entity.Customer;
import com.bbpms.order.mapper.CustomerMapper;
import com.bbpms.order.service.CustomerService;
import com.bbpms.order.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Customer master-data service implementation.
 *
 * <p>Encryption strategy: SM4 (CBC, padding) with the application key from
 * {@link OrderProperties#getSm4Key()}. Key rotation should be accompanied
 * by a re-encryption job that walks every customer row.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final OrderProperties orderProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsert(Customer req) {
        if (req == null || req.getPhone() == null || req.getPhone().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "客户手机号必填");
        }

        String key = orderProperties.getSm4Key();
        String phoneCipher = CryptoUtils.sm4Encrypt(req.getPhone(), key);

        // Idempotency by phone — exactly one row per UK.
        Customer existing = customerMapper.selectByPhone(phoneCipher);
        if (existing != null) {
            log.debug("Reusing existing customer id={} by phone", existing.getId());
            return existing.getId();
        }

        // Optional second check by idCardNo.
        if (req.getIdCardNo() != null && !req.getIdCardNo().isBlank()) {
            String idCardCipher = CryptoUtils.sm4Encrypt(req.getIdCardNo(), key);
            existing = customerMapper.selectByIdCardNo(idCardCipher);
            if (existing != null) {
                return existing.getId();
            }
            req.setIdCardNo(idCardCipher);
        } else {
            req.setIdCardNo(null);
        }

        // Encrypt remaining PII.
        req.setPhone(phoneCipher);
        if (req.getName() != null && !req.getName().isBlank()) {
            req.setName(CryptoUtils.sm4Encrypt(req.getName(), key));
        }

        if (req.getStatus() == null) {
            req.setStatus(1);
        }
        customerMapper.insert(req);
        log.info("Created customer id={} phone-tail=****",
                req.getId());
        return req.getId();
    }

    @Override
    public CustomerVO getById(Long id, boolean mask) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw new BizException(ResultCode.CUSTOMER_NOT_FOUND);
        }
        // Default behaviour per the spec: mask=true for security.
        return toVO(c, !mask);
    }

    @Override
    public Customer findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String key = orderProperties.getSm4Key();
        String phoneCipher = CryptoUtils.sm4Encrypt(phone, key);
        return customerMapper.selectByPhone(phoneCipher);
    }

    @Override
    public PageResp<CustomerVO> page(int pageNum, int pageSize, String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        // PII is SM4 ciphertext so keyword matching cannot run in SQL — load
        // the page from all non-deleted rows, filter in memory, then re-paginate
        // so both `total` and `records` reflect the filtered set. Master-data
        // scale is small; if it grows, move to a plaintext search column.
        List<Customer> all = customerMapper.selectList(null);
        List<Customer> matched = all;
        if (!kw.isEmpty()) {
            matched = all.stream().filter(c -> matchesKw(c, kw)).toList();
        }
        int total = matched.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        List<Customer> pageRows = from >= to ? List.of() : matched.subList(from, to);

        PageResp<CustomerVO> resp = new PageResp<>();
        resp.setPageNum((long) pageNum);
        resp.setPageSize((long) pageSize);
        resp.setTotal((long) total);
        resp.setPages((long) Math.ceil(total / (double) Math.max(1, pageSize)));
        resp.setRecords(pageRows.stream().map(c -> toVO(c, false)).toList());
        return resp;
    }

    private boolean matchesKw(Customer c, String kw) {
        String key = orderProperties.getSm4Key();
        try {
            String name = c.getName() == null ? "" : CryptoUtils.sm4Decrypt(c.getName(), key);
            String phone = c.getPhone() == null ? "" : CryptoUtils.sm4Decrypt(c.getPhone(), key);
            return name.toLowerCase().contains(kw) || phone.toLowerCase().contains(kw);
        } catch (Exception ex) {
            log.warn("Decrypt failed for customer id={} in page filter", c.getId(), ex);
            // Fall back to matching on raw stored value (e.g. legacy plaintext rows).
            String raw = (c.getName() == null ? "" : c.getName()) + (c.getPhone() == null ? "" : c.getPhone());
            return raw.toLowerCase().contains(kw);
        }
    }

    @Override
    public List<CustomerVO> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || limit <= 0) {
            return Collections.emptyList();
        }
        String kw = keyword.trim().toLowerCase();
        String key = orderProperties.getSm4Key();
        List<CustomerVO> result = new ArrayList<>();
        // PII columns are SM4 ciphertext, so LIKE cannot run in SQL — match
        // in memory after decrypt. Fine at master-data scale; if the table
        // grows, move to a plaintext hash column or a search index.
        for (Customer c : customerMapper.selectList(null)) {
            String name = null;
            String phone = null;
            try {
                name = c.getName() == null ? "" : CryptoUtils.sm4Decrypt(c.getName(), key);
                phone = c.getPhone() == null ? "" : CryptoUtils.sm4Decrypt(c.getPhone(), key);
            } catch (Exception ex) {
                // Legacy plaintext row — match against the raw stored value.
                name = c.getName();
                phone = c.getPhone();
            }
            boolean hit = (name != null && name.toLowerCase().contains(kw))
                    || (phone != null && phone.toLowerCase().contains(kw));
            if (hit) {
                result.add(toVO(c, false));
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private CustomerVO toVO(Customer c, boolean unmasked) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(c, vo);
        vo.setMasked(!unmasked);
        String key = orderProperties.getSm4Key();
        // For each PII field: try decrypt; if decryption fails (e.g. legacy
        // plaintext seed rows), fall back to the raw stored value so the UI
        // never shows ciphertext garbage.
        if (c.getName() != null) {
            String plain = decryptSafe(c.getName(), key, c.getId());
            vo.setName(plain == null ? c.getName() : (unmasked ? plain : CryptoUtils.maskName(plain)));
        }
        if (c.getPhone() != null) {
            String plain = decryptSafe(c.getPhone(), key, c.getId());
            vo.setPhone(plain == null ? c.getPhone() : (unmasked ? plain : CryptoUtils.maskPhone(plain)));
        }
        if (c.getIdCardNo() != null) {
            String plain = decryptSafe(c.getIdCardNo(), key, c.getId());
            vo.setIdCardNo(plain == null ? c.getIdCardNo() : (unmasked ? plain : CryptoUtils.maskIdCard(plain)));
        }
        return vo;
    }

    /**
     * Decrypt with key, returning {@code null} when the value is not valid
     * ciphertext (legacy plaintext rows) or any decryption error occurs.
     */
    private String decryptSafe(String value, String key, Long customerId) {
        try {
            return CryptoUtils.sm4Decrypt(value, key);
        } catch (Exception ex) {
            log.warn("Decrypt failed for customer id={}, falling back to raw value", customerId);
            return null;
        }
    }
}
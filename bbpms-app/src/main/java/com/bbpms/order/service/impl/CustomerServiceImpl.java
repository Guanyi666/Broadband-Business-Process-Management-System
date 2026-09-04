package com.bbpms.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public PageResp<CustomerVO> page(int pageNum, int pageSize) {
        Page<Customer> p = customerMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Customer>().orderByDesc(Customer::getCreateTime));
        PageResp<CustomerVO> resp = new PageResp<>();
        resp.setPageNum(p.getCurrent());
        resp.setPageSize(p.getSize());
        resp.setTotal(p.getTotal());
        resp.setPages(p.getPages());
        resp.setRecords(p.getRecords().stream().map(c -> toVO(c, false)).toList());
        return resp;
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
            String name = decryptOrLegacyPlaintext(c.getName(), key);
            String phone = decryptOrLegacyPlaintext(c.getPhone(), key);
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
        if (c.getName() != null) {
            String plain = decryptOrLegacyPlaintext(c.getName(), key);
            vo.setName(unmasked ? plain : CryptoUtils.maskPhone(plain));
        }
        if (c.getPhone() != null) {
            String plain = decryptOrLegacyPlaintext(c.getPhone(), key);
            vo.setPhone(unmasked ? plain : CryptoUtils.maskPhone(plain));
        }
        if (c.getIdCardNo() != null) {
            String plain = decryptOrLegacyPlaintext(c.getIdCardNo(), key);
            vo.setIdCardNo(unmasked ? plain : CryptoUtils.maskIdCard(plain));
        }
        return vo;
    }

    /** Fresh rows are encrypted; legacy demo seed rows may still be plaintext. */
    private String decryptOrLegacyPlaintext(String value, String key) {
        if (value == null) return null;
        try {
            return CryptoUtils.sm4Decrypt(value, key);
        } catch (Exception ignored) {
            return value;
        }
    }
}

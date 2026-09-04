package com.bbpms.order.service;

import com.bbpms.common.result.PageResp;
import com.bbpms.order.entity.Customer;
import com.bbpms.order.vo.CustomerVO;

import java.util.List;

/**
 * Customer master-data service.
 *
 * <p>Sensitive fields ({@code name}, {@code idCardNo}, {@code phone}) are
 * encrypted at rest with SM4 and masked on read unless the caller has the
 * {@code customer:view-sensitive} permission.</p>
 */
public interface CustomerService {

    /**
     * Idempotent upsert: if an existing customer shares the supplied
     * phone (idempotency key), return the existing id without modification;
     * otherwise persist a new customer with encrypted PII.
     *
     * @param req raw (plaintext) customer draft
     * @return customer id (existing or newly minted)
     */
    Long upsert(Customer req);

    /**
     * Get a customer by id.
     *
     * @param id       customer primary key
     * @param mask     if {@code true} (default), PII fields are returned
     *                 masked; if {@code false}, plaintext is returned and
     *                 the caller MUST have the {@code customer:view-sensitive}
     *                 permission
     */
    CustomerVO getById(Long id, boolean mask);

    /**
     * Find a customer by phone (exact, on the encrypted column).
     *
     * @return customer or {@code null}
     */
    Customer findByPhone(String phone);

    /**
     * Paged customer list (masked PII).
     *
     * @param keyword optional fuzzy filter on decrypted name / phone;
     *                {@code null}/{@code blank} returns the whole page.
     */
    PageResp<CustomerVO> page(int pageNum, int pageSize, String keyword);

    /**
     * Fuzzy search on decrypted name/phone (masked PII), capped at
     * {@code limit} hits. PII columns are SM4 ciphertext so matching is
     * done in memory — acceptable at master-data scale.
     */
    List<CustomerVO> search(String keyword, int limit);
}
package com.bbpms.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbpms.order.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Customer mapper. Encrypted-by-SM4 lookups on phone / idCardNo drive the
 * idempotent upsert in {@code CustomerService.upsert}.
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * Look up by ciphertext of id-card-no. Returns first match.
     */
    Customer selectByIdCardNo(@Param("idCardNoCipher") String idCardNoCipher);

    /**
     * Look up by ciphertext of phone. Returns first match.
     */
    Customer selectByPhone(@Param("phoneCipher") String phoneCipher);

    /**
     * Bulk fetch by id list — used by the order detail composite to
     * attach encrypted rows without N+1 queries.
     */
    List<Customer> selectByIds(@Param("ids") List<Long> ids);
}

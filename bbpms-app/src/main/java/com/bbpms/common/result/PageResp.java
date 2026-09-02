package com.bbpms.common.result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageResp<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private long pageNum;
    private long pageSize;
    private long total;
    private long pages;
    private List<T> records;

    public static <T> PageResp<T> of(IPage<T> page) {
        PageResp<T> p = new PageResp<>();
        p.setPageNum(page.getCurrent());
        p.setPageSize(page.getSize());
        p.setTotal(page.getTotal());
        p.setPages(page.getPages());
        p.setRecords(page.getRecords());
        return p;
    }
    public static <T> PageResp<T> empty(long pageNum, long pageSize) {
        PageResp<T> p = new PageResp<>();
        p.setPageNum(pageNum);
        p.setPageSize(pageSize);
        p.setTotal(0);
        p.setPages(0);
        p.setRecords(Collections.emptyList());
        return p;
    }
}

package com.bbpms.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bbpms.user.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> list();

    List<SysDept> getTree();
}
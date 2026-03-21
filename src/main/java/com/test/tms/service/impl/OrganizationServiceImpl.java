package com.test.tms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.test.tms.entity.Organization;
import com.test.tms.mapper.OrganizationMapper;
import com.test.tms.service.OrganizationService;
import org.springframework.stereotype.Service;

@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization>
        implements OrganizationService {
}


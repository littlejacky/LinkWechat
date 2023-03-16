package com.linkwechat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linkwechat.domain.fission.WeFissionDetail;
import org.apache.ibatis.annotations.Param;

/**
* @author robin
* @description 针对表【we_fission_detail(裂变明细表)】的数据库操作Service
* @createDate 2023-03-14 14:07:21
*/
public interface IWeFissionDetailService extends IService<WeFissionDetail> {
    void physicalDelete(Long fissionId);
}

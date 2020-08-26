package com.linkwechat.wecom.service;

import java.util.List;
import com.linkwechat.wecom.domain.WxCorpAccount;

/**
 * 企业id相关配置Service接口
 * 
 * @author ruoyi
 * @date 2020-08-24
 */
public interface IWxCorpAccountService 
{
    /**
     * 查询企业id相关配置
     * 
     * @param id 企业id相关配置ID
     * @return 企业id相关配置
     */
    public WxCorpAccount selectWxCorpAccountById(Long id);

    /**
     * 查询企业id相关配置列表
     * 
     * @param wxCorpAccount 企业id相关配置
     * @return 企业id相关配置集合
     */
    public List<WxCorpAccount> selectWxCorpAccountList(WxCorpAccount wxCorpAccount);

    /**
     * 新增企业id相关配置
     * 
     * @param wxCorpAccount 企业id相关配置
     * @return 结果
     */
    public int insertWxCorpAccount(WxCorpAccount wxCorpAccount);

    /**
     * 修改企业id相关配置
     * 
     * @param wxCorpAccount 企业id相关配置
     * @return 结果
     */
    public int updateWxCorpAccount(WxCorpAccount wxCorpAccount);

    /**
     * 批量删除企业id相关配置
     * 
     * @param ids 需要删除的企业id相关配置ID
     * @return 结果
     */
    public int deleteWxCorpAccountByIds(Long[] ids);

    /**
     * 删除企业id相关配置信息
     * 
     * @param id 企业id相关配置ID
     * @return 结果
     */
    public int deleteWxCorpAccountById(Long id);
}

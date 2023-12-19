package com.linkwechat.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.linkwechat.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 关键词群访问统计
 * @TableName we_keyword_group_view_count
 */
@Data
@TableName(value ="we_keyword_group_view_count")
public class WeKeywordGroupViewCount extends BaseEntity {
    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 访问数量
     */
    private Long viewNum;

    /**
     * 关键词主表主键
     */
    private Long keywordGroupId;

    /**
     * unionld
     */
    private String unionId;

    /**
     * 0:正常;1:删除;
     */
    @TableLogic
    private Integer delFlag;


}
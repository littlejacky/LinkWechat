package com.linkwechat.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linkwechat.domain.WeProduct;
import com.linkwechat.domain.product.query.WeProductQuery;
import com.linkwechat.domain.product.vo.WeProductListVo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品信息表(WeProduct)
 *
 * @author danmo
 * @since 2022-09-30 11:36:06
 */
@Repository()
@Mapper
public interface WeProductMapper extends BaseMapper<WeProduct> {


    List<WeProductListVo> queryProductList(WeProductQuery query);
}


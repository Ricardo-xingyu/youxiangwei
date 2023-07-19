package cn.lili.controller.goods;

import cn.lili.common.enums.ResultCode;
import cn.lili.common.enums.ResultUtil;
import cn.lili.common.exception.ServiceException;
import cn.lili.common.vo.PageVO;
import cn.lili.common.vo.ResultMessage;
import cn.lili.modules.goods.entity.dos.Goods;
import cn.lili.modules.goods.entity.dto.GoodsSearchParams;
import cn.lili.modules.goods.entity.vos.GoodsVO;
import cn.lili.modules.goods.service.GoodsService;
import cn.lili.modules.goods.service.GoodsSkuService;
import cn.lili.modules.search.entity.dos.EsGoodsIndex;
import cn.lili.modules.search.entity.dos.EsGoodsRelatedInfo;
import cn.lili.modules.search.entity.dto.EsGoodsSearchDTO;
import cn.lili.modules.search.service.EsGoodsSearchService;
import cn.lili.modules.search.service.HotWordsService;
import cn.lili.modules.statistics.aop.PageViewPoint;
import cn.lili.modules.statistics.aop.enums.PageViewEnum;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 买家端,商品接口
 *
 * @author Chopper
 * @since 2020/11/16 10:06 下午
 */
@Slf4j
@Api(tags = "买家端,商品接口")
@RestController
@RequestMapping("/buyer/goods/goods")
public class GoodsBuyerController {

    /**
     * 商品
     */
    @Autowired
    private GoodsService goodsService;
    /**
     * 商品SKU
     */
    @Autowired
    private GoodsSkuService goodsSkuService;
    /**
     * ES商品搜索
     */
    @Autowired
    private EsGoodsSearchService goodsSearchService;

    @Autowired
    private HotWordsService hotWordsService;

    @ApiOperation(value = "通过id获取商品信息")
    @ApiImplicitParam(name = "goodsId", value = "商品ID", required = true, paramType = "path", dataType = "Long")
    @GetMapping(value = "/get/{goodsId}")
    public ResultMessage<GoodsVO> get(@NotNull(message = "商品ID不能为空") @PathVariable("goodsId") String id) {
        return ResultUtil.data(goodsService.getGoodsVO(id));
    }

    @ApiOperation(value = "通过id获取商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "goodsId", value = "商品ID", required = true, paramType = "path"),
            @ApiImplicitParam(name = "skuId", value = "skuId", required = true, paramType = "path")
    })
    @GetMapping(value = "/sku/{goodsId}/{skuId}")
    @PageViewPoint(type = PageViewEnum.SKU, id = "#id")
    public ResultMessage<Map<String, Object>> getSku(@NotNull(message = "商品ID不能为空") @PathVariable("goodsId") String goodsId,
                                                     @NotNull(message = "SKU ID不能为空") @PathVariable("skuId") String skuId) {
        try {
            // 读取选中的列表
            Map<String, Object> map = goodsSkuService.getGoodsSkuDetail(goodsId, skuId);
            return ResultUtil.data(map);
        } catch (ServiceException se) {
            log.info(se.getMsg(), se);
            throw se;
        } catch (Exception e) {
            log.error(ResultCode.GOODS_ERROR.message(), e);
            return ResultUtil.error(ResultCode.GOODS_ERROR);
        }

    }

    @ApiOperation(value = "获取商品分页列表")
    @GetMapping
    public ResultMessage<IPage<Goods>> getByPage(GoodsSearchParams goodsSearchParams) {
        return ResultUtil.data(goodsService.queryByParams(goodsSearchParams));
    }

    @ApiOperation(value = "从ES中获取商品信息")
    @GetMapping("/es")
    public ResultMessage<content<EsGoodsIndex>> getGoodsByPageFromEs(EsGoodsSearchDTO goodsSearchParams, PageVO pageVO) {
        pageVO.setNotConvert(true);
        SearchPage<EsGoodsIndex> esGoodsIndices = goodsSearchService.searchGoods(goodsSearchParams, pageVO);
        /*
        以下为修改的：将原本从es中查询到的商品列表中的销量，覆盖为直接从mysql中查询的，这样就可以直接修改数据库达到修改销量的效果
         */
        // 定义一个inner类，在同级目录下，因为es查询出来的对象无法进行set，所以构造一个一样结构的对象用于返回
        List<SearchHit<EsGoodsIndex>> searchHits = esGoodsIndices.getSearchHits().getSearchHits();
        List<inner<EsGoodsIndex>> list = new ArrayList<>();
        content<EsGoodsIndex> ans = new content<>();
        for (SearchHit<EsGoodsIndex> index : searchHits) {
            EsGoodsIndex esGood = index.getContent();
            // 得到商品的id
            String id = esGood.getGoodsId();
            // 根据id查询商品的销量
            int buyCount = goodsService.getBuyCountById(id);
            // 更新商品的销量
            esGood.setBuyCount(buyCount);
            // 将所有的值赋值到新的inner中，再返回
            inner<EsGoodsIndex> a = new inner<>();
            a.setIndex(index.getIndex());
            a.setId(index.getId());
            a.setScore(index.getScore());
            a.setSortValues(index.getSortValues());
            a.setContent(esGood);
            a.setHighlightFields(index.getHighlightFields());
            a.setInnerHits(index.getInnerHits());
            a.setNestedMetaData(index.getNestedMetaData());
            a.setFlag("true");
            list.add(a);
        }
        ans.setContent(list);
        return ResultUtil.data(ans);
        /*
        修改结束
         */
//        return ResultUtil.data(esGoodsIndices);
    }

    @ApiOperation(value = "从ES中获取相关商品品牌名称，分类名称及属性")
    @GetMapping("/es/related")
    public ResultMessage<EsGoodsRelatedInfo> getGoodsRelatedByPageFromEs(EsGoodsSearchDTO goodsSearchParams, PageVO pageVO) {
        pageVO.setNotConvert(true);
        EsGoodsRelatedInfo selector = goodsSearchService.getSelector(goodsSearchParams, pageVO);
        return ResultUtil.data(selector);
    }

    @ApiOperation(value = "获取搜索热词")
    @GetMapping("/hot-words")
    public ResultMessage<List<String>> getGoodsHotWords(Integer count) {
        List<String> hotWords = hotWordsService.getHotWords(count);
        return ResultUtil.data(hotWords);
    }

}

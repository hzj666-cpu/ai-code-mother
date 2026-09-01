#set(tableComment = table.getComment())
#set(primaryKeyType = table.getPrimaryKey().getPropertySimpleType())
#set(entityClassName = table.buildEntityClassName())
#set(entityVarName = firstCharToLowerCase(entityClassName))
#set(serviceVarName = firstCharToLowerCase(table.buildServiceClassName()))
package #(packageConfig.controllerPackage);

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import #(packageConfig.entityPackage).#(entityClassName);
import #(packageConfig.servicePackage).#(table.buildServiceClassName());
import com.hzj.aicodemother.common.BaseResponse;
import com.hzj.aicodemother.common.ResultUtils;
#if(controllerConfig.restStyle)
import org.springframework.web.bind.annotation.RestController;
#else
import org.springframework.stereotype.Controller;
#end
#if(controllerConfig.superClass != null)
import #(controllerConfig.buildSuperClassImport());
#end
#if(withSwagger && swaggerVersion.getName() == "FOX")
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
#end
#if(withSwagger && swaggerVersion.getName() == "DOC")
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
#end
import java.util.List;

/**
 * #(tableComment) 控制层。
 *
#if(javadocConfig.getAuthor())
 * @author #(javadocConfig.getAuthor())
#end
#if(javadocConfig.getSince())
 * @since #(javadocConfig.getSince())
#end
 */
#if(controllerConfig.restStyle)
@RestController
#else
@Controller
#end
#if(withSwagger && swaggerVersion.getName() == "FOX")
@Api("#(tableComment)接口")
#end
#if(withSwagger && swaggerVersion.getName() == "DOC")
@Tag(name = "#(tableComment)接口")
#end
@RequestMapping("#(table.buildControllerRequestMappingPrefix())/#(firstCharToLowerCase(entityClassName))")
public class #(table.buildControllerClassName()) #if(controllerConfig.superClass)extends #(controllerConfig.buildSuperClassName()) #end {

    @Autowired
    private #(table.buildServiceClassName()) #(serviceVarName);

    /**
     * 新增#(tableComment)。
     *
     * @param #(entityVarName) #(tableComment)
     * @return 新增成功返回 id
     */
    @PostMapping("add")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("新增#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "新增#(tableComment)")
    #end
    public BaseResponse<Boolean> add(#if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)") #end @RequestBody #(entityClassName) #(entityVarName)) {
        return ResultUtils.success(#(serviceVarName).save(#(entityVarName)));
    }

    /**
     * 批量新增#(tableComment)。
     *
     * @param #(entityVarName)List #(tableComment)列表
     * @return {@code true} 批量保存成功，{@code false} 批量保存失败
     */
    @PostMapping("add/batch")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("批量新增#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "批量新增#(tableComment)")
    #end
    public BaseResponse<Boolean> addBatch(#if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)列表") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)列表") #end @RequestBody List<#(entityClassName)> #(entityVarName)List) {
        return ResultUtils.success(#(serviceVarName).saveBatch(#(entityVarName)List));
    }

    /**
     * 根据主键删除#(tableComment)。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("根据主键删除#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "根据主键删除#(tableComment)")
    #end
    public BaseResponse<Boolean> remove(@PathVariable #if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)主键") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)主键") #end #(primaryKeyType) id) {
        return ResultUtils.success(#(serviceVarName).removeById(id));
    }

    /**
     * 根据主键列表批量删除#(tableComment)。
     *
     * @param ids 主键列表
     * @return {@code true} 批量删除成功，{@code false} 批量删除失败
     */
    @DeleteMapping("remove/batch")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("根据主键列表批量删除#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "根据主键列表批量删除#(tableComment)")
    #end
    public BaseResponse<Boolean> removeBatch(#if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)主键列表") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)主键列表") #end @RequestBody List<#(primaryKeyType)> ids) {
        return ResultUtils.success(#(serviceVarName).removeByIds(ids));
    }

    /**
     * 根据主键更新#(tableComment)。
     *
     * @param #(entityVarName) #(tableComment)
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("根据主键更新#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "根据主键更新#(tableComment)")
    #end
    public BaseResponse<Boolean> update(#if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)") #end @RequestBody #(entityClassName) #(entityVarName)) {
        return ResultUtils.success(#(serviceVarName).updateById(#(entityVarName)));
    }

    /**
     * 查询所有#(tableComment)。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("查询所有#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "查询所有#(tableComment)")
    #end
    public BaseResponse<List<#(entityClassName)>> list() {
        return ResultUtils.success(#(serviceVarName).list());
    }

    /**
     * 根据主键列表查询#(tableComment)。
     *
     * @param ids 主键列表，如 ?ids=1,2,3
     * @return #(tableComment)列表
     */
    @GetMapping("list/ids")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("根据主键列表查询#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "根据主键列表查询#(tableComment)")
    #end
    public BaseResponse<List<#(entityClassName)>> listByIds(#if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)主键列表") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)主键列表") #end @RequestParam List<#(primaryKeyType)> ids) {
        return ResultUtils.success(#(serviceVarName).listByIds(ids));
    }

    /**
     * 根据主键获取#(tableComment)。
     *
     * @param id #(tableComment)主键
     * @return #(tableComment)详情
     */
    @GetMapping("get/{id}")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("根据主键获取#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "根据主键获取#(tableComment)")
    #end
    public BaseResponse<#(entityClassName)> getInfo(@PathVariable #if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("#(tableComment)主键") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "#(tableComment)主键") #end #(primaryKeyType) id) {
        return ResultUtils.success(#(serviceVarName).getById(id));
    }

    /**
     * 分页查询#(tableComment)。
     *
     * @param pageNum  当前页号，从 1 开始，默认 1
     * @param pageSize 每页大小，默认 10
     * @return 分页结果
     */
    @GetMapping("page")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("分页查询#(tableComment)")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "分页查询#(tableComment)")
    #end
    public BaseResponse<Page<#(entityClassName)>> page(#if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("页号，从 1 开始") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "页号，从 1 开始") #end @RequestParam(defaultValue = "1") long pageNum,
            #if(withSwagger && swaggerVersion.getName() == "FOX")@ApiParam("每页数量") #end #if(withSwagger && swaggerVersion.getName() == "DOC")@Parameter(description = "每页数量") #end @RequestParam(defaultValue = "10") long pageSize) {
        Page<#(entityClassName)> page = new Page<>(pageNum, pageSize);
        return ResultUtils.success(#(serviceVarName).page(page));
    }

    /**
     * 统计#(tableComment)总数。
     *
     * @return 总数
     */
    @GetMapping("count")
    #if(withSwagger && swaggerVersion.getName() == "FOX")
    @ApiOperation("统计#(tableComment)总数")
    #end
    #if(withSwagger && swaggerVersion.getName() == "DOC")
    @Operation(description = "统计#(tableComment)总数")
    #end
    public BaseResponse<Long> count() {
        return ResultUtils.success(#(serviceVarName).count());
    }

}

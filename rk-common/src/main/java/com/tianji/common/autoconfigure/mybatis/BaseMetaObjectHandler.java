package com.tianji.common.autoconfigure.mybatis;


import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.tianji.common.utils.NumberUtils;
import com.tianji.common.utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_CREATER;
import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_UPDATER;
import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_CREATE_TIME_CAMEL;
import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_UPDATE_TIME_CAMEL;


/**
 * 操作数据库前自动填充需要更新的内容
 * 只支持单个对象，不支持批量插入更新时的填充
 *
 * @author RK-Web Team
 * @since 1.0.0
 **/
public class BaseMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        //创建人
        setCreater(metaObject);

        //更新人
        setUpdater(metaObject);

        //创建时间
        setCreateTime(metaObject);

        //更新时间
        setUpdateTime(metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //更新数据时，修改更新人
        setUpdater(metaObject);

        //更新时间
        setUpdateTime(metaObject);
    }

    private void setCreater(MetaObject metaObject) {
        Long userId = UserContext.getUser();
        //未找到用户id默认0
        this.strictInsertFill(metaObject, DATA_FIELD_NAME_CREATER, Long.class, NumberUtils.null2Zero(userId)); // 起始版本 3.3.0(推荐使用)
    }

    private void setUpdater(MetaObject metaObject) {
        Long userId = UserContext.getUser();
        //未找到用户id默认0
        this.strictUpdateFill(metaObject, DATA_FIELD_NAME_UPDATER, Long.class, NumberUtils.null2Zero(userId)); // 起始版本 3.3.0(推荐使用)
    }

    private void setCreateTime(MetaObject metaObject) {
        this.strictInsertFill(metaObject, DATA_FIELD_NAME_CREATE_TIME_CAMEL, LocalDateTime.class, LocalDateTime.now()); // 起始版本 3.3.0(推荐使用)
    }

    private void setUpdateTime(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, DATA_FIELD_NAME_UPDATE_TIME_CAMEL, LocalDateTime.class, LocalDateTime.now()); // 起始版本 3.3.0(推荐使用)
    }
}

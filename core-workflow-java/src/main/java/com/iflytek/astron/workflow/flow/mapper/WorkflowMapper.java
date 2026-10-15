package com.iflytek.astron.workflow.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.astron.workflow.flow.entity.WorkflowEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Workflow mapper
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowEntity> {
}

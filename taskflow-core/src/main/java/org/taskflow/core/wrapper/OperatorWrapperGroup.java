package org.taskflow.core.wrapper;

import org.taskflow.core.DagEngine;
import org.taskflow.core.operator.IOperator;

/**
 * OP节点组，将多个节点抽象成一个组，可以简化对节点依赖的管理，尤其是DAG中节点比较多时
 * 根据依赖关系划分成多个组，每个组内的节点可以单独管理，比如系统中涉及多个模块，每个模块又有多个OP节点
 * Created by ytyht226 on 2022/4/6.
 */
@SuppressWarnings("all")
public class OperatorWrapperGroup extends OperatorWrapper{
    /**
     * 节点组开始节点OP
     */
    private static final GroupBeginOperator groupBeginOperator = new GroupBeginOperator();
    /**
     * 节点组结束节点OP
     */
    private static final GroupEndOperator groupEndOperator = new GroupEndOperator();
    /**
     * DAG执行引擎
     */
    private DagEngine engine;
    /**
     * 分组的开始节点，相当于原始开始节点的代理，只进行参数传递
     */
    private OperatorWrapper groupBegin;
    /**
     * 分组的结束节点，相当于原始结束节点的代理，只进行参数传递
     */
    private OperatorWrapper groupEnd;
    /**
     * 分组的开始节点id
     */
    private String groupBeginId;
    /**
     * 分组的结束节点id
     */
    private String groupEndId;
    /**
     * 原始的开始节点
     */
    private String[] beginWrapperIds;
    /**
     * 原始的结束节点
     */
    private String[] endWrapperIds;

    public OperatorWrapperGroup() {

    }

    public OperatorWrapperGroup(DagEngine engine) {
        this.engine = engine;
    }

    /**
     * 初始化，根据原始开始/结束节点，生成对应的节点组的开始/结束节点，只初始化一次
     */
    public OperatorWrapperGroup init() {
        if (this.isInit()) {
            return this;
        }
        this.setInit(true);
        groupBeginId = beginWrapperIds == null ? buildGroupId(true, String.valueOf(this.hashCode())) : groupBeginId;
        groupEndId = endWrapperIds == null ? buildGroupId(false, String.valueOf(this.hashCode())) : groupEndId;
        beginWrapperIds = beginWrapperIds == null ? new String[]{groupEndId} : beginWrapperIds;
        endWrapperIds = endWrapperIds == null ? new String[]{groupBeginId} : endWrapperIds;

        //根据原始开始节点，生成对应的节点组的开始节点
        OperatorWrapper groupBegin = new OperatorWrapper(groupBeginId, groupBeginOperator);
        groupBegin.id(groupBeginId)
                .engine(engine)
                .next(beginWrapperIds);
        engine.getWrapperMap().put(groupBeginId, groupBegin);
        this.groupBegin = groupBegin;

        //根据原始结束节点，生成对应的节点组的结束节点
        OperatorWrapper groupEnd = new OperatorWrapper(groupEndId, groupEndOperator);
        groupEnd.id(groupEndId)
                .engine(engine)
                .addParamFromWrapperId(endWrapperIds);
        engine.getWrapperMap().put(groupEndId, groupEnd);
        this.groupEnd = groupEnd;

        buildGroupBegin(beginWrapperIds);
        buildGroupEnd(endWrapperIds);
        return this;
    }

    public OperatorWrapper addParam(Object... params) {
        groupBegin.addParam(params);
        return this;
    }
    /**
     * 节点组也是一种特殊的节点，可以添加依赖的参数来源
     */
    public OperatorWrapper addParamFromWrapperId(String... fromWrapperIds) {
        groupBegin.addParamFromWrapperId(fromWrapperIds);
        return this;
    }

    /**
     * 添加节点组结束节点的后继节点
     */
    public OperatorWrapper next(String... wrapperIds) {
        this.init();
        engine.getWrapperMap().get(groupEndId).next(wrapperIds);
        return this;
    }

    public OperatorWrapperGroup beginWrapperIds(String... wrapperIds) {
        this.beginWrapperIds = wrapperIds;
        this.groupBeginId = buildGroupId(true, beginWrapperIds);
        return this;
    }

    public OperatorWrapperGroup endWrapperIds(String... wrapperIds) {
        this.endWrapperIds = wrapperIds;
        this.groupEndId = buildGroupId(false, endWrapperIds);
        return this;
    }

    /**
     * 设置节点组开始节点
     */
    private OperatorWrapperGroup buildGroupBegin(String... nextWrapperIds) {

        for (String wrapperId : nextWrapperIds) {
            OperatorWrapper<?, ?> wrapper = engine.getWrapperMap().get(wrapperId);
            if (wrapper.getParamFromList() == null && wrapper.getParamList() == null) {
                try {
                    wrapper.getOperator().getClass().getDeclaredMethod("execute", Void.class);
                } catch (NoSuchMethodException e) {
                    //execute 接口有入参，且节点没有指定参数来源时，设置参数来源是节点组的开始节点
                    wrapper.addParamFromWrapperId(groupBeginId);
                }
            }
        }
        return this;
    }

    /**
     * 设置节点组结束节点
     */
    private OperatorWrapperGroup buildGroupEnd(String... dependWrapperIds) {

        for (String wrapperId : dependWrapperIds) {
            engine.getWrapperMap().get(wrapperId).next(groupEnd.getId());
        }
        return this;
    }

    /**
     * 构造开始/结束节点的id
     */
    private String buildGroupId(boolean isBegin, String... ids) {
        if (ids == null) {
            return null;
        }
        String prefix = isBegin ? "begin" : "end";
        StringBuilder builder = new StringBuilder(prefix);
        for (String id : ids) {
            builder.append("_").append(id);
        }
        return builder.toString();
    }

    /**
     * 节点组的开始节点OP
     */
    private static class GroupBeginOperator implements IOperator {

        @Override
        public Object execute(Object param) throws Exception {
            return param;
        }
    }

    /**
     * 节点组的结束节点OP
     */
    private static class GroupEndOperator implements IOperator {

        @Override
        public Object execute(Object param) throws Exception {
            return param;
        }
    }

    public String getGroupBeginId() {
        return groupBeginId;
    }

    public String getGroupEndId() {
        return groupEndId;
    }

    public String[] getBeginWrapperIds() {
        return beginWrapperIds;
    }

    public void setBeginWrapperIds(String[] beginWrapperIds) {
        this.beginWrapperIds = beginWrapperIds;
    }

    public String[] getEndWrapperIds() {
        return endWrapperIds;
    }

    public void setEndWrapperIds(String[] endWrapperIds) {
        this.endWrapperIds = endWrapperIds;
    }

    public OperatorWrapper getGroupBegin() {
        return groupBegin;
    }
}
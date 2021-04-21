package com.linkwechat.wecom.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linkwechat.common.core.domain.entity.WeCorpAccount;
import com.linkwechat.common.enums.ChatType;
import com.linkwechat.common.enums.MediaType;
import com.linkwechat.common.enums.TaskSendType;
import com.linkwechat.common.enums.CommunityTaskType;
import com.linkwechat.common.exception.wecom.WeComException;
import com.linkwechat.common.utils.SecurityUtils;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.common.utils.bean.BeanUtils;
import com.linkwechat.wecom.client.WeCustomerMessagePushClient;
import com.linkwechat.wecom.client.WeMessagePushClient;
import com.linkwechat.wecom.domain.*;
import com.linkwechat.wecom.domain.dto.WeMediaDto;
import com.linkwechat.wecom.domain.dto.WeMessagePushDto;
import com.linkwechat.wecom.domain.dto.WePresTagGroupTaskDto;
import com.linkwechat.wecom.domain.dto.message.*;
import com.linkwechat.wecom.domain.vo.WeCommunityTaskEmplVo;
import com.linkwechat.wecom.domain.vo.WePresTagGroupTaskStatVo;
import com.linkwechat.wecom.domain.vo.WePresTagGroupTaskVo;
import com.linkwechat.wecom.mapper.*;
import com.linkwechat.wecom.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WePresTagGroupTaskServiceImpl extends ServiceImpl<WePresTagGroupTaskMapper, WePresTagGroupTask> implements IWePresTagGroupTaskService {

    @Autowired
    private WePresTagGroupTaskMapper taskMapper;

    @Autowired
    private WePresTagGroupTaskStatMapper taskStatMapper;

    @Autowired
    private WePresTagGroupTaskScopeMapper taskScopeMapper;

    @Autowired
    private WePresTagGroupTaskTagMapper taskTagMapper;

    @Autowired
    private WeCustomerMessagePushClient customerMessagePushClient;

    @Autowired
    private WeMessagePushClient messagePushClient;

    @Autowired
    private IWeMaterialService materialService;

    @Autowired
    private WeGroupCodeMapper groupCodeMapper;

    @Autowired
    private IWeCorpAccountService corpAccountService;

    @Autowired
    private WeCustomerMapper customerMapper;

    @Value("${wecome.authorizeUrl}")
    private String authorizeUrl;

    @Value("${wecome.authorizeRedirectUrl}")
    private String authorizeRedirectUrl;

    /**
     * 添加新标签建群任务
     * @param task 建群任务本体信息
     * @param tagIdList 标签列表
     * @param emplIdList 员工列表
     * @return 结果
     */
    @Override
    @Transactional
    public int add(WePresTagGroupTask task, List<String> tagIdList, List<String> emplIdList) {
        if (taskMapper.insertTask(task) > 0) {
            // 保存标签对象
            if (CollectionUtil.isNotEmpty(tagIdList)) {
                List<WePresTagGroupTaskTag> taskTagList = tagIdList
                        .stream()
                        .map(id -> new WePresTagGroupTaskTag(task.getTaskId(), id))
                        .collect(Collectors.toList());
                taskTagMapper.batchBindsTaskTags(taskTagList);
            }

            // 保存员工信息
            if (CollectionUtil.isNotEmpty(emplIdList)) {
                List<WePresTagGroupTaskScope> wePresTagGroupTaskScopeList = emplIdList
                        .stream()
                        .map(id -> new WePresTagGroupTaskScope(task.getTaskId(), id, false))
                        .collect(Collectors.toList());
                taskScopeMapper.batchBindsTaskScopes(wePresTagGroupTaskScopeList);
            }
            return 1;
        }
        return 0;
    }

    /**
     * 根据条件查询任务列表
     *
     * @param taskName  任务名称
     * @param sendType  发送方式
     * @param createBy  创建人
     * @param beginTime 起始时间
     * @param endTime   结束时间
     * @return 结果
     */
    @Override
    public List<WePresTagGroupTaskVo> selectTaskList(String taskName, Integer sendType, String createBy, String beginTime, String endTime) {
        // 查询任务列表
        List<WePresTagGroupTaskVo> taskVoList = taskMapper.selectTaskList(taskName, sendType, createBy, beginTime, endTime);
        if (CollectionUtil.isNotEmpty(taskVoList)) {
            taskVoList.forEach(this::setGroupCodeAndScopeAndTag);
        }
        return taskVoList;
    }

    /**
     * 通过id获取老客标签建群任务
     *
     * @param taskId 任务id
     * @return 结果
     */
    @Override
    public WePresTagGroupTaskVo getTaskById(Long taskId) {
        WePresTagGroupTaskVo taskVo = taskMapper.selectTaskById(taskId);
        if (StringUtils.isNotNull(taskVo)) {
            setGroupCodeAndScopeAndTag(taskVo);
        }
        return taskVo;
    }

    /**
     * 批量删除老客标签建群任务
     *
     * @param idList 任务id列表
     * @return 删除的行数
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public int batchRemoveTaskByIds(Long[] idList) {
        List<Long> ids = Arrays.asList(idList);

        // 解除关联的标签
        QueryWrapper<WePresTagGroupTaskTag> taskTagQueryWrapper = new QueryWrapper<>();
        taskTagQueryWrapper.in("task_id", ids);
        taskTagMapper.delete(taskTagQueryWrapper);
        // 解除关联的员工
        QueryWrapper<WePresTagGroupTaskScope> taskScopeQueryWrapper = new QueryWrapper<>();
        taskScopeQueryWrapper.in("task_id", ids);
        taskScopeMapper.delete(taskScopeQueryWrapper);
        // 删除其用户统计
        QueryWrapper<WePresTagGroupTaskStat> statQueryWrapper = new QueryWrapper<>();
        statQueryWrapper.in("task_id", ids);
        taskStatMapper.delete(statQueryWrapper);

        // 删除task
        QueryWrapper<WePresTagGroupTask> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.in("task_id", ids);
        return taskMapper.delete(taskQueryWrapper);
    }

    /**
     * 更新老客户标签建群任务
     *
     * @param wePresTagGroupTaskDto 更新数据
     * @return 结果
     * @taskId 待更新任务id
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public int updateTask(Long taskId, WePresTagGroupTaskDto wePresTagGroupTaskDto) {
        WePresTagGroupTask wePresTagGroupTask = new WePresTagGroupTask();
        BeanUtils.copyProperties(wePresTagGroupTaskDto, wePresTagGroupTask);
        wePresTagGroupTask.setTaskId(taskId);
        wePresTagGroupTask.setUpdateBy(SecurityUtils.getUsername());
        if (taskMapper.updateTask(wePresTagGroupTask) > 0) {
            // 更新标签
            // 先删除旧标签
            QueryWrapper<WePresTagGroupTaskTag> taskTagQueryWrapper = new QueryWrapper<>();
            taskTagQueryWrapper.eq("task_id", taskId);
            taskTagMapper.delete(taskTagQueryWrapper);
            // 再添加新标签
            List<String> tagIdList = wePresTagGroupTaskDto.getTagList();
            if (CollectionUtil.isNotEmpty(tagIdList)) {
                List<WePresTagGroupTaskTag> wePresTagGroupTaskTagList = tagIdList.stream().map(id -> new WePresTagGroupTaskTag(taskId, id)).collect(Collectors.toList());
                taskTagMapper.batchBindsTaskTags(wePresTagGroupTaskTagList);
            }

            // 先解除旧的员工绑定信息
            QueryWrapper<WePresTagGroupTaskScope> taskScopeQueryWrapper = new QueryWrapper<>();
            taskScopeQueryWrapper.eq("task_id", taskId);
            taskScopeMapper.delete(taskScopeQueryWrapper);

            // 再重新绑定员工信息
            List<String> userIdList = wePresTagGroupTaskDto.getScopeList();
            if (CollectionUtil.isNotEmpty(userIdList)) {
                List<WePresTagGroupTaskScope> wePresTagGroupTaskScopeList = userIdList.stream().map(id -> new WePresTagGroupTaskScope(taskId, id, false)).collect(Collectors.toList());
                taskScopeMapper.batchBindsTaskScopes(wePresTagGroupTaskScopeList);
            }
            return 1;
        }
        return 0;
    }

    /**
     * 检测任务名是否已存在
     *
     * @param taskName 任务名
     * @return 结果
     */
    @Override
    public boolean checkTaskNameUnique(String taskName) {
        int count = taskMapper.checkTaskNameUnique(taskName);
        return count <= 0;
    }

    /**
     * 通过老客标签建群id获取其统计信息
     *
     * @param taskId 任务id
     * @return 统计信息
     */
    @Override
    public List<WePresTagGroupTaskStatVo> getStatByTaskId(Long taskId) {
        WePresTagGroupTask task = taskMapper.selectById(taskId);
        // 该任务对应的所有外部联系人id
        List<String> externalIdList = taskStatMapper.getAllExternalIdByTaskId(taskId);
        List<WePresTagGroupTaskStatVo> statVoList = new ArrayList<>();
        if (task.getSendType().equals(TaskSendType.CROP.getType())) {
            // 企业群发。通过企微接口统计
            QueryCustomerMessageStatusResultDataObjectDto requestData = new QueryCustomerMessageStatusResultDataObjectDto();
            requestData.setMsgid(task.getMsgid());
            QueryCustomerMessageStatusResultDto resultDto = customerMessagePushClient.queryCustomerMessageStatus(requestData);

            if (StringUtils.isNotEmpty(resultDto.getDetail_list())) {
                for (DetailMessageStatusResultDto detail : resultDto.getDetail_list()) {
                    WePresTagGroupTaskStatVo statVo = new WePresTagGroupTaskStatVo();
                    WeCustomer customer = customerMapper.selectWeCustomerById(detail.getExternal_userid());
                    statVo.setCustomerName(customer.getName());
                    statVo.setStatus(detail.getStatus());
                    statVo.setInGroup(externalIdList.contains(detail.getExternal_userid()));
                    statVoList.add(statVo);
                }
            }

        } else {
            // 个人群发。通过数据库进行统计
            statVoList = taskStatMapper.selectStatInfoByTaskId(taskId);
        }
        return statVoList;

    }

    /**
     * 根据任务id获取对应员工信息列表
     *
     * @param taskId 任务id
     * @return 结果
     */
    @Override
    public List<WeCommunityTaskEmplVo> getScopeListByTaskId(Long taskId) {
        return taskScopeMapper.getScopeListByTaskId(taskId);
    }

    /**
     * 根据任务id获取对应标签信息列表
     *
     * @param taskId 任务id
     * @return 结果
     */
    @Override
    public List<WeTag> getTagListByTaskId(Long taskId) {
        return taskTagMapper.getTagListByTaskId(taskId);
    }

    /**
     * 获取员工建群任务信息
     *
     * @param emplId 员工id
     * @param isDone 是否已处理
     * @return 结果
     */
    @Override
    public List<WePresTagGroupTaskVo> getEmplTaskList(String emplId, boolean isDone) {
        List<WePresTagGroupTaskVo> taskVoList = taskMapper.getTaskListByEmplId(emplId, isDone);
        if (StringUtils.isNotEmpty(taskVoList)) {
            taskVoList.forEach(this::setGroupCodeAndScopeAndTag);
        }
        return taskVoList;
    }

    /**
     * 员工发送信息后，变更其任务状态为 "完成"
     *
     * @param taskId 任务id
     * @param emplId 员工id
     * @return 结果
     */
    @Override
    public int updateEmplTaskStatus(Long taskId, String emplId) {
        return taskScopeMapper.updateEmplTaskStatus(taskId, emplId);
    }

    /**
     * 任务群活码、员工和标签
     *
     * @param taskVo 任务vo
     */
    private void setGroupCodeAndScopeAndTag(WePresTagGroupTaskVo taskVo) {
        // 任务群活码信息
        taskVo.fillGroupCodeVo();
        // 员工信息
        taskVo.setScopeList(this.getScopeListByTaskId(taskVo.getTaskId()));
        // 客户标签
        taskVo.setTagList(this.getTagListByTaskId(taskVo.getTaskId()));
    }

    /**
     * 任务派发
     *
     * @param task 建群任务
     */
    @Override
    @Async
    public void sendMessage(WePresTagGroupTask task) {
        try {

            Integer sendType = task.getSendType();

            if (sendType.equals(TaskSendType.CROP.getType())) {
                // 企业群发
                this.sendCorpMessage(task);

            } else {
                // 个人群发
                this.sendEmployeeMessage(task);
            }

        } catch (Exception e) {
            throw new WeComException(e.getMessage());
        }
    }

    /**
     * 企业群发
     *
     * @param task 建群任务信息
     */
    private void sendCorpMessage(WePresTagGroupTask task) {
        try {

            // 构建企微api参数 [客户联系 - 消息推送 - 创建企业群发]

            // 群发任务的类型、外部联系人id列表
            List<String> externalIdList = taskTagMapper.getExternalUserIdListByTaskId(task.getTaskId());
            WeCustomerMessagePushDto queryData = new WeCustomerMessagePushDto();
            queryData.setChat_type(ChatType.SINGLE.getName());
            queryData.setExternal_userid(externalIdList);

            // 引导语
            TextMessageDto text = new TextMessageDto();
            text.setContent(task.getWelcomeMsg());
            queryData.setText(text);

            // 群活码图片（上传临时文件获取media_id） TODO 过期问题？
            ImageMessageDto image = new ImageMessageDto();
            WeGroupCode groupCode = groupCodeMapper.selectWeGroupCodeById(task.getGroupCodeId());
            WeMediaDto mediaDto = materialService.uploadTemporaryMaterial(groupCode.getCodeUrl(), MediaType.IMAGE.getMediaType(), "临时文件");
            image.setMedia_id(mediaDto.getMedia_id());
            queryData.setImage(image);

            // 调用企业群发接口
            SendMessageResultDto resultDto = customerMessagePushClient.sendCustomerMessageToUser(queryData);

            // 设定该任务的msgid
            UpdateWrapper<WePresTagGroupTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("task_id", task.getTaskId());
            updateWrapper.set("msgid", resultDto.getMsgid());
            this.update(updateWrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 个人群发
     *
     * @param task 建群任务信息
     */
    private void sendEmployeeMessage(WePresTagGroupTask task) {
        WeMessagePushDto pushDto = new WeMessagePushDto();
        // 设置toUser参数
        List<WeCommunityTaskEmplVo> employeeList = taskScopeMapper.getScopeListByTaskId(task.getTaskId());
        String toUser = employeeList.stream().map(WeCommunityTaskEmplVo::getUserId).collect(Collectors.joining("|"));
        pushDto.setTouser(toUser);

        // 获取agentId
        WeCorpAccount validWeCorpAccount = corpAccountService.findValidWeCorpAccount();
        String agentId = validWeCorpAccount.getAgentId();
        String corpId = validWeCorpAccount.getCorpId();
        if (StringUtils.isEmpty(agentId)) {
            throw new WeComException("当前agentId不可用或不存在");
        }
        pushDto.setAgentid(Integer.valueOf(agentId));

        // 设置文本消息
        TextMessageDto text = new TextMessageDto();
        String REDIRECT_URI = URLEncoder.encode(String.format("%s?corpId=%s&agentId=%s&type=%s", authorizeRedirectUrl, corpId, agentId, CommunityTaskType.TAG.getType()));
        String context = String.format(
                "你有一个新任务，<a href='%s?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect'>请点击此链接查看</a>",
                authorizeUrl, corpId, REDIRECT_URI);
        text.setContent(context);
        pushDto.setText(text);

        pushDto.setMsgtype("text");

        // 请求消息推送接口，获取结果 [消息推送 - 发送应用消息]
        messagePushClient.sendMessageToUser(pushDto, agentId);
    }
}
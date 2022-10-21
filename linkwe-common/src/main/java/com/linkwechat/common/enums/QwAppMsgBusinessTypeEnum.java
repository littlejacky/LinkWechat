package com.linkwechat.common.enums;

import lombok.Getter;

/**
 * @author sxw
 * @description
 * @date 2022/4/14 18:37
 **/
public enum QwAppMsgBusinessTypeEnum {

    COMMON(1, "CommonAppMsgService", "默认通知任务"),
    ;


    @Getter
    private int type;
    @Getter
    private String beanName;
    @Getter
    private String desc;

    QwAppMsgBusinessTypeEnum(int type, String beanName, String desc) {
        this.type = type;
        this.beanName = beanName;
        this.desc = desc;
    }

    public static QwAppMsgBusinessTypeEnum parseEnum(int type) {
        QwAppMsgBusinessTypeEnum[] alarmGrades = QwAppMsgBusinessTypeEnum.values();
        for (int i = 0; i < alarmGrades.length; i++) {
            if (alarmGrades[i].getType() == type) {
                return alarmGrades[i];
            }
        }
        return null;
    }
}

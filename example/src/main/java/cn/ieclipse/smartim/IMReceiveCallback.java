package cn.ieclipse.smartim;

import cn.ieclipse.smartim.callback.ReceiveCallback;
import cn.ieclipse.smartim.common.LOG;
import cn.ieclipse.smartim.console.IMChatConsole;
import cn.ieclipse.smartim.model.impl.AbstractContact;
import cn.ieclipse.smartim.model.impl.AbstractFrom;
import cn.ieclipse.smartim.model.impl.AbstractMessage;
import cn.ieclipse.smartim.views.IMPanel;
import cn.ieclipse.util.EncodeUtils;
import com.scienjus.smartqq.model.*;
import io.github.biezhi.wechat.model.UserFrom;
import io.github.biezhi.wechat.model.WechatMessage;

import java.io.IOException;

public abstract class IMReceiveCallback implements ReceiveCallback {
    protected IMChatConsole lastConsole;
    protected IMPanel fContactView;

    public IMReceiveCallback(IMPanel fContactView) {
        this.fContactView = fContactView;
    }

    protected abstract String getNotifyContent(AbstractMessage message,
                                               AbstractFrom from);

    protected abstract String getMsgContent(AbstractMessage message,
                                            AbstractFrom from);

    protected void handle(boolean unknown, boolean notify,
                          AbstractMessage message, AbstractFrom from,
                          AbstractContact contact) {
        SmartClient client = fContactView.getClient();
        String msg = getMsgContent(message, from);
        if (!unknown) {
            String hf = EncodeUtils.getMd5(from.getContact().getName());
            IMHistoryManager.getInstance().save(client, hf, msg);
        }

        notify2ubuntu(message, from, contact);

//        if (notify) {
//            boolean hide = unknown
//                    && !SmartIMSettings.getInstance().getState().NOTIFY_UNKNOWN;
//            try {
//                hide = hide || from.getMember().getUin()
//                        .equals(fContactView.getClient().getAccount().getUin());
//            } catch (Exception e) {
//            }
//            if (hide || fContactView.isCurrent(contact)) {
//                // don't notify
//            } else {
//                CharSequence content = getNotifyContent(message, from);
//                Notifications.notify(fContactView, contact, contact.getName(),
//                        content);
//            }
//        }

        IMChatConsole console = fContactView.findConsoleById(contact.getUin(),
                false);
        if (console != null) {
            lastConsole = console;
            console.write(msg);
            fContactView.highlight(console);
        }
        if (!fContactView.isCurrent(console)) {
            if (contact != null) {
                contact.increaceUnRead();
            }
        }

        if (contact != null) {
            contact.setLastMessage(message);
        }

        fContactView.notifyUpdateContacts(0, false);
    }

    private void notify2ubuntu(AbstractMessage message, AbstractFrom from, AbstractContact contact) {
        // 添加linux的桌面提醒
        StringBuffer command = new StringBuffer("notify-send ");
        if (message instanceof WechatMessage) {
            String wechatImgPath = "/home/tony/pictures/wechat.png";
            command.append("-i ").append(wechatImgPath);
            if (from instanceof io.github.biezhi.wechat.model.GroupFrom) {
                io.github.biezhi.wechat.model.GroupFrom _from = (io.github.biezhi.wechat.model.GroupFrom) from;
                command.append(" ")
                        .append(_from.getGroup())
                        .append(" ")
                        .append(_from.getUser().NickName)
                        .append("：")
                        .append(((WechatMessage) message).Content);
            } else if (from instanceof UserFrom && from.getTarget() == null) {
                UserFrom _from = (UserFrom) from;
                command.append(" ")
                        .append(_from.getName())
                        .append(" ")
                        .append(((WechatMessage) message).Content);
            }
        } else if (message instanceof QQMessage) {
            String qqImgPath = "/home/tony/pictures/qq.png";
            command.append("-i ").append(qqImgPath);
            if (from instanceof com.scienjus.smartqq.model.GroupFrom) {
                com.scienjus.smartqq.model.GroupFrom _from = (com.scienjus.smartqq.model.GroupFrom) from;
                command.append(" ")
                        .append(_from.getGroup())
                        .append(" ")
                        .append(_from.getName())
                        .append("：")
                        .append(((QQMessage) message).getContent());
            } else if (from instanceof DiscussFrom) {
                DiscussFrom _from = (DiscussFrom) from;
                command.append(" ")
                        .append(_from.getDiscuss())
                        .append(" ")
                        .append(_from.getName())
                        .append("：")
                        .append(((QQMessage) message).getContent());
            } else if (from instanceof FriendFrom) {
                FriendFrom _from = (FriendFrom) from;
                command.append(" ")
                        .append(_from.getName())
                        .append(" ")
                        .append(((FriendMessage) message).getContent());
            }
        }
        if (command.toString().equals("notify-send ")) return;

        try {
            Runtime.getRuntime().exec(command.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveError(Throwable e) {
        if (e == null) {
            return;
        }
        if (lastConsole != null) {
            lastConsole.error(e);
        } else {
            LOG.error("微信接收异常" + e);
            LOG.sendNotification("错误", e.getMessage());
        }
    }
}

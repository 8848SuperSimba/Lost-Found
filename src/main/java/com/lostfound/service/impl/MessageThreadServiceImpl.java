package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.common.ResultCode;
import com.lostfound.dto.CreateThreadRequest;
import com.lostfound.dto.SendMessageRequest;
import com.lostfound.entity.ItemPost;
import com.lostfound.entity.Message;
import com.lostfound.entity.MessageThread;
import com.lostfound.entity.User;
import com.lostfound.enums.NotificationType;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.enums.ThreadStatus;
import com.lostfound.exception.BusinessException;
import com.lostfound.mapper.ItemPostMapper;
import com.lostfound.mapper.MessageMapper;
import com.lostfound.mapper.MessageThreadMapper;
import com.lostfound.mapper.UserMapper;
import com.lostfound.service.MessageThreadService;
import com.lostfound.service.NotificationService;
import com.lostfound.vo.MessageVO;
import com.lostfound.vo.ThreadVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MessageThreadServiceImpl implements MessageThreadService {

    private final MessageThreadMapper messageThreadMapper;
    private final MessageMapper messageMapper;
    private final ItemPostMapper itemPostMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public MessageThreadServiceImpl(
            MessageThreadMapper messageThreadMapper,
            MessageMapper messageMapper,
            ItemPostMapper itemPostMapper,
            UserMapper userMapper,
            NotificationService notificationService) {
        this.messageThreadMapper = messageThreadMapper;
        this.messageMapper = messageMapper;
        this.itemPostMapper = itemPostMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThreadVO createThread(CreateThreadRequest request, Long currentUserId) {
        ItemPost lostPost = itemPostMapper.selectById(request.getLostPostId());
        ItemPost foundPost = itemPostMapper.selectById(request.getFoundPostId());
        if (lostPost == null || foundPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (lostPost.getPostType() != PostType.LOST || foundPost.getPostType() != PostType.FOUND) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "必须由一个失物帖和一个寻物帖建立会话");
        }
        if (lostPost.getStatus() == PostStatus.CLOSED || foundPost.getStatus() == PostStatus.CLOSED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帖子已关闭，无法建立会话");
        }
        if (!currentUserId.equals(lostPost.getPublisherUserId()) && !currentUserId.equals(foundPost.getPublisherUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限建立该会话");
        }

        MessageThread existed = messageThreadMapper.selectOne(new LambdaQueryWrapper<MessageThread>()
                .eq(MessageThread::getLostPostId, request.getLostPostId())
                .eq(MessageThread::getFoundPostId, request.getFoundPostId())
                .last("LIMIT 1"));
        if (existed != null) {
            return buildThreadVO(existed, currentUserId, false);
        }

        Long receiverUserId = currentUserId.equals(lostPost.getPublisherUserId())
                ? foundPost.getPublisherUserId()
                : lostPost.getPublisherUserId();

        MessageThread thread = MessageThread.builder()
                .lostPostId(request.getLostPostId())
                .foundPostId(request.getFoundPostId())
                .initiatorUserId(currentUserId)
                .receiverUserId(receiverUserId)
                .status(ThreadStatus.ACTIVE)
                .build();
        messageThreadMapper.insert(thread);

        itemPostMapper.updateById(ItemPost.builder().id(lostPost.getId()).status(PostStatus.MATCHED).build());
        itemPostMapper.updateById(ItemPost.builder().id(foundPost.getId()).status(PostStatus.MATCHED).build());

        User initiator = userMapper.selectById(currentUserId);
        ItemPost receiverPost = currentUserId.equals(lostPost.getPublisherUserId()) ? foundPost : lostPost;
        String initiatorNickname = initiator == null ? "匿名用户" : initiator.getNickname();
        notificationService.createNotification(
                receiverUserId,
                NotificationType.SYSTEM,
                "有人想与你联系",
                "用户「" + initiatorNickname + "」就帖子「" + receiverPost.getTitle() + "」发起了联系，请及时查看",
                "THREAD",
                thread.getId());

        return buildThreadVO(thread, currentUserId, false);
    }

    @Override
    public List<ThreadVO> listThreads(Long currentUserId) {
        List<MessageThread> threads = messageThreadMapper.selectList(new LambdaQueryWrapper<MessageThread>()
                .and(wrapper -> wrapper.eq(MessageThread::getInitiatorUserId, currentUserId)
                        .or()
                        .eq(MessageThread::getReceiverUserId, currentUserId))
                .orderByDesc(MessageThread::getUpdatedAt));
        return threads.stream().map(thread -> buildThreadVO(thread, currentUserId, false)).toList();
    }

    @Override
    public ThreadVO getThreadDetail(Long threadId, Long currentUserId) {
        MessageThread thread = checkThreadParticipant(threadId, currentUserId);
        return buildThreadVO(thread, currentUserId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<MessageVO> getMessages(Long threadId, Long currentUserId, int page, int size) {
        checkThreadParticipant(threadId, currentUserId);

        Page<Message> messagePage = messageMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getThreadId, threadId)
                        .orderByAsc(Message::getCreatedAt));

        List<Message> unreadMessages = messagePage.getRecords().stream()
                .filter(message ->
                        !currentUserId.equals(message.getSenderUserId()) && (message.getIsRead() == null || message.getIsRead() == 0))
                .toList();
        if (!unreadMessages.isEmpty()) {
            List<Long> ids = unreadMessages.stream().map(Message::getId).toList();
            messageMapper.update(
                    Message.builder().isRead(1).build(),
                    new LambdaQueryWrapper<Message>().in(Message::getId, ids));
            unreadMessages.forEach(message -> message.setIsRead(1));
        }

        List<MessageVO> records = messagePage.getRecords().stream()
                .map(message -> toMessageVO(message, currentUserId))
                .toList();
        Page<MessageVO> result = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long threadId, SendMessageRequest request, Long currentUserId) {
        MessageThread thread = checkThreadParticipant(threadId, currentUserId);
        if (thread.getStatus() == ThreadStatus.CLOSED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话已关闭，无法发送消息");
        }

        Message message = Message.builder()
                .threadId(threadId)
                .senderUserId(currentUserId)
                .content(request.getContent())
                .isRead(0)
                .build();
        messageMapper.insert(message);

        messageThreadMapper.updateById(MessageThread.builder()
                .id(threadId)
                .updatedAt(LocalDateTime.now())
                .build());

        Long otherUserId = currentUserId.equals(thread.getInitiatorUserId()) ? thread.getReceiverUserId() : thread.getInitiatorUserId();
        String notificationContent = request.getContent().length() > 20
                ? request.getContent().substring(0, 20) + "..."
                : request.getContent();
        notificationService.createNotification(
                otherUserId,
                NotificationType.MESSAGE,
                "收到新消息",
                notificationContent,
                "THREAD",
                threadId);

        return toMessageVO(message, currentUserId);
    }

    private MessageThread checkThreadParticipant(Long threadId, Long currentUserId) {
        MessageThread thread = messageThreadMapper.selectById(threadId);
        if (thread == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!currentUserId.equals(thread.getInitiatorUserId()) && !currentUserId.equals(thread.getReceiverUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限访问该会话");
        }
        return thread;
    }

    private ThreadVO buildThreadVO(MessageThread thread, Long currentUserId, boolean includeContacts) {
        ItemPost lostPost = itemPostMapper.selectById(thread.getLostPostId());
        ItemPost foundPost = itemPostMapper.selectById(thread.getFoundPostId());

        Long otherUserId = currentUserId.equals(thread.getInitiatorUserId()) ? thread.getReceiverUserId() : thread.getInitiatorUserId();
        User otherUser = userMapper.selectById(otherUserId);

        ItemPost myPost = null;
        ItemPost otherPost = null;
        if (lostPost != null && currentUserId.equals(lostPost.getPublisherUserId())) {
            myPost = lostPost;
            otherPost = foundPost;
        } else if (foundPost != null && currentUserId.equals(foundPost.getPublisherUserId())) {
            myPost = foundPost;
            otherPost = lostPost;
        }

        Message lastMessage = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getThreadId, thread.getId())
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT 1"));
        String lastMessageContent = lastMessage == null ? null : truncate(lastMessage.getContent(), 30);
        LocalDateTime lastMessageTime = lastMessage == null ? thread.getCreatedAt() : lastMessage.getCreatedAt();

        Long unreadCount = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getThreadId, thread.getId())
                .ne(Message::getSenderUserId, currentUserId)
                .eq(Message::getIsRead, 0));

        return ThreadVO.builder()
                .id(thread.getId())
                .lostPostId(thread.getLostPostId())
                .foundPostId(thread.getFoundPostId())
                .otherUserId(otherUserId)
                .otherNickname(otherUser == null ? null : otherUser.getNickname())
                .otherAvatar(otherUser == null ? null : otherUser.getAvatarUrl())
                .relatedPostTitle(myPost == null ? null : myPost.getTitle())
                .lastMessageContent(lastMessageContent)
                .lastMessageTime(lastMessageTime)
                .unreadCount(unreadCount)
                .status(thread.getStatus())
                .otherContactInfo(includeContacts && otherPost != null ? otherPost.getContactInfo() : null)
                .myContactInfo(includeContacts && myPost != null ? myPost.getContactInfo() : null)
                .build();
    }

    private MessageVO toMessageVO(Message message, Long currentUserId) {
        User sender = userMapper.selectById(message.getSenderUserId());
        return MessageVO.builder()
                .id(message.getId())
                .threadId(message.getThreadId())
                .senderUserId(message.getSenderUserId())
                .senderNickname(sender == null ? null : sender.getNickname())
                .senderAvatar(sender == null ? null : sender.getAvatarUrl())
                .content(message.getContent())
                .isRead(message.getIsRead() != null && message.getIsRead() == 1)
                .createdAt(message.getCreatedAt())
                .isSelf(currentUserId.equals(message.getSenderUserId()))
                .build();
    }

    private String truncate(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }
}

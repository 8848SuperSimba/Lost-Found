package com.lostfound.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.dto.CreateThreadRequest;
import com.lostfound.dto.SendMessageRequest;
import com.lostfound.vo.MessageVO;
import com.lostfound.vo.ThreadVO;
import java.util.List;

public interface MessageThreadService {

    ThreadVO createThread(CreateThreadRequest request, Long currentUserId);

    List<ThreadVO> listThreads(Long currentUserId);

    ThreadVO getThreadDetail(Long threadId, Long currentUserId);

    Page<MessageVO> getMessages(Long threadId, Long currentUserId, int page, int size);

    MessageVO sendMessage(Long threadId, SendMessageRequest request, Long currentUserId);
}

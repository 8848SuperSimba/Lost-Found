package com.lostfound.service;

import com.lostfound.vo.MatchVO;
import java.util.List;

public interface MatchService {

    void triggerMatchAsync(Long newPostId);

    List<MatchVO> reMatchPost(Long postId, Long currentUserId);

    List<MatchVO> getMatchResults(Long postId, Long currentUserId);

    void triggerAllOpenPostsAsync();
}

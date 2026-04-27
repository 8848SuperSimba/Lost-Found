package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.entity.ItemPost;
import com.lostfound.entity.MatchResult;
import com.lostfound.entity.User;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.enums.UserRole;
import com.lostfound.enums.UserStatus;
import com.lostfound.mapper.ItemPostMapper;
import com.lostfound.mapper.MatchResultMapper;
import com.lostfound.mapper.UserMapper;
import com.lostfound.service.NotificationService;
import com.lostfound.service.WxService;
import com.lostfound.util.WxUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WxServiceImpl implements WxService {

    private final UserMapper userMapper;
    private final ItemPostMapper itemPostMapper;
    private final MatchResultMapper matchResultMapper;
    private final NotificationService notificationService;

    @Value("${wx.token}")
    private String wxToken;

    @Value("${wx.appid}")
    private String wxAppid;

    public WxServiceImpl(
            UserMapper userMapper,
            ItemPostMapper itemPostMapper,
            MatchResultMapper matchResultMapper,
            NotificationService notificationService) {
        this.userMapper = userMapper;
        this.itemPostMapper = itemPostMapper;
        this.matchResultMapper = matchResultMapper;
        this.notificationService = notificationService;
    }

    @Override
    public String verify(String signature, String timestamp, String nonce, String echostr) {
        boolean valid = WxUtil.verifySha1(wxToken, timestamp, nonce, signature);
        return valid ? echostr : "forbidden";
    }

    @Override
    public String handleMessage(String xmlBody) {
        Map<String, String> bodyMap = WxUtil.parseXml(xmlBody);
        String openid = bodyMap.get("FromUserName");
        String toUserName = bodyMap.get("ToUserName");
        String msgType = bodyMap.get("MsgType");
        String event = bodyMap.get("Event");
        String content = bodyMap.get("Content");

        if ("event".equals(msgType) && "subscribe".equals(event)) {
            return handleSubscribe(openid, toUserName);
        }
        if ("text".equals(msgType)) {
            String input = content == null ? "" : content.trim();
            if ("查询".equals(input) || "匹配".equals(input)) {
                return handleQuery(openid, toUserName);
            }
            if ("我的帖子".equals(input)) {
                return handleMyPosts(openid, toUserName);
            }
        }
        return buildTextReply(openid, toUserName, defaultGuideText());
    }

    private String handleSubscribe(String openid, String toUserName) {
        User user = findUserByOpenid(openid);
        if (user == null) {
            User newUser = User.builder()
                    .wxOpenid(openid)
                    .nickname("微信用户")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userMapper.insert(newUser);
        }
        String welcomeText = "欢迎关注校园失物招领系统！\n\n支持以下指令：\n"
                + "【查询】查看您的最新匹配结果\n"
                + "【我的帖子】查看您发布的帖子";
        return buildTextReply(openid, toUserName, welcomeText);
    }

    private String handleQuery(String openid, String toUserName) {
        User user = findUserByOpenid(openid);
        if (user == null) {
            return buildTextReply(openid, toUserName, "未找到您的账号，请先在网站注册并关联微信");
        }

        List<ItemPost> openPosts = itemPostMapper.selectList(new LambdaQueryWrapper<ItemPost>()
                .eq(ItemPost::getPublisherUserId, user.getId())
                .eq(ItemPost::getStatus, PostStatus.OPEN));
        if (openPosts == null || openPosts.isEmpty()) {
            return buildTextReply(openid, toUserName, "您当前没有进行中的帖子");
        }

        List<PostMatchItem> matchItems = new ArrayList<>();
        for (ItemPost post : openPosts) {
            MatchResult topMatch = matchResultMapper.selectOne(new LambdaQueryWrapper<MatchResult>()
                    .eq(MatchResult::getSrcPostId, post.getId())
                    .orderByDesc(MatchResult::getScore)
                    .last("LIMIT 1"));
            if (topMatch != null && topMatch.getScore() != null) {
                matchItems.add(new PostMatchItem(post.getTitle(), topMatch.getScore()));
            }
        }

        if (matchItems.isEmpty()) {
            return buildTextReply(openid, toUserName, "您当前没有进行中的帖子");
        }

        matchItems.sort(Comparator.comparing(PostMatchItem::score).reversed());
        StringBuilder sb = new StringBuilder();
        sb.append("您有 ").append(matchItems.size()).append(" 个帖子有匹配结果：\n");
        int limit = Math.min(5, matchItems.size());
        for (int i = 0; i < limit; i++) {
            PostMatchItem item = matchItems.get(i);
            int percent = item.score().multiply(BigDecimal.valueOf(100)).intValue();
            sb.append(i + 1)
                    .append(".【")
                    .append(item.title())
                    .append("】匹配度 ")
                    .append(percent)
                    .append("%\n");
        }
        sb.append("请登录网站查看详细信息");
        if (matchItems.size() > 5) {
            sb.append("\n（仅显示前5条）");
        }
        return buildTextReply(openid, toUserName, sb.toString());
    }

    private String handleMyPosts(String openid, String toUserName) {
        User user = findUserByOpenid(openid);
        if (user == null) {
            return buildTextReply(openid, toUserName, "未找到您的账号，请先在网站注册并关联微信");
        }

        List<ItemPost> posts = itemPostMapper.selectList(new LambdaQueryWrapper<ItemPost>()
                .eq(ItemPost::getPublisherUserId, user.getId())
                .in(ItemPost::getStatus, PostStatus.OPEN, PostStatus.MATCHED)
                .orderByDesc(ItemPost::getCreatedAt)
                .last("LIMIT 10"));

        if (posts == null || posts.isEmpty()) {
            return buildTextReply(openid, toUserName, "您当前没有进行中的帖子");
        }

        StringBuilder sb = new StringBuilder("您的帖子列表：\n");
        for (int i = 0; i < posts.size(); i++) {
            ItemPost post = posts.get(i);
            sb.append(i + 1)
                    .append(".【")
                    .append(postTypeText(post.getPostType()))
                    .append("】")
                    .append(post.getTitle())
                    .append(" - ")
                    .append(statusText(post.getStatus()));
            if (i < posts.size() - 1) {
                sb.append("\n");
            }
        }
        return buildTextReply(openid, toUserName, sb.toString());
    }

    private User findUserByOpenid(String openid) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getWxOpenid, openid));
    }

    private String postTypeText(PostType postType) {
        if (postType == PostType.LOST) {
            return "失物";
        }
        if (postType == PostType.FOUND) {
            return "寻物";
        }
        return "";
    }

    private String statusText(PostStatus status) {
        if (status == PostStatus.OPEN) {
            return "进行中";
        }
        if (status == PostStatus.MATCHED) {
            return "已匹配";
        }
        return "";
    }

    private String defaultGuideText() {
        return "您好！支持以下指令：\n"
                + "【查询】查看您的最新匹配结果\n"
                + "【我的帖子】查看您发布的帖子\n\n"
                + "如需更多功能，请访问网站";
    }

    private String buildTextReply(String toUser, String fromUser, String content) {
        long createTime = System.currentTimeMillis() / 1000;
        String safeToUser = toUser == null ? "" : toUser;
        String safeFromUser = fromUser == null ? wxAppid : fromUser;
        String safeContent = content == null ? "" : content;
        return String.format(
                "<xml>"
                        + "<ToUserName><![CDATA[%s]]></ToUserName>"
                        + "<FromUserName><![CDATA[%s]]></FromUserName>"
                        + "<CreateTime>%d</CreateTime>"
                        + "<MsgType><![CDATA[text]]></MsgType>"
                        + "<Content><![CDATA[%s]]></Content>"
                        + "</xml>",
                safeToUser, safeFromUser, createTime, safeContent);
    }

    private record PostMatchItem(String title, BigDecimal score) {}
}

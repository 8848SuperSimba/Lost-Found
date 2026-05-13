package com.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.dto.CreatePostRequest;
import com.lostfound.entity.ItemPost;
import com.lostfound.entity.ItemKeyword;
import com.lostfound.entity.MatchResult;
import com.lostfound.entity.User;
import com.lostfound.enums.ItemCategory;
import com.lostfound.enums.PostStatus;
import com.lostfound.enums.PostType;
import com.lostfound.mapper.ItemKeywordMapper;
import com.lostfound.mapper.ItemPostMapper;
import com.lostfound.mapper.MatchResultMapper;
import com.lostfound.mapper.UserMapper;
import com.lostfound.service.ItemPostService;
import com.lostfound.service.WxService;
import com.lostfound.util.WxUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxServiceImpl implements WxService {

    private static final String FLOW_KEY = "flow";
    private static final String STEP_KEY = "step";
    private static final String DATA_KEY = "data";
    private static final String FLOW_ADMIN_POST = "ADMIN_POST";
    private static final String FLOW_SEARCH = "SEARCH";
    private static final String FLOW_BIND = "BIND";
    private static final String STEP_ADMIN_TYPE = "ADMIN_TYPE";
    private static final String STEP_ADMIN_TITLE = "ADMIN_TITLE";
    private static final String STEP_ADMIN_CATEGORY = "ADMIN_CATEGORY";
    private static final String STEP_ADMIN_AREA_CODE = "ADMIN_AREA_CODE";
    private static final String STEP_ADMIN_AREA_TEXT = "ADMIN_AREA_TEXT";
    private static final String STEP_ADMIN_TIME = "ADMIN_TIME";
    private static final String STEP_ADMIN_DESC = "ADMIN_DESC";
    private static final String STEP_ADMIN_LOCATION = "ADMIN_LOCATION";
    private static final String STEP_ADMIN_CONTACT = "ADMIN_CONTACT";
    private static final String STEP_ADMIN_KEYWORDS = "ADMIN_KEYWORDS";
    private static final String STEP_ADMIN_IMAGES = "ADMIN_IMAGES";
    private static final String STEP_SEARCH_KEYWORDS = "SEARCH_KEYWORDS";
    private static final String STEP_SEARCH_AREA = "SEARCH_AREA";
    private static final String STEP_SEARCH_TIME = "SEARCH_TIME";
    private static final String STEP_BIND_IDENTIFIER = "BIND_IDENTIFIER";
    private static final String STEP_BIND_PASSWORD = "BIND_PASSWORD";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATETIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[,，\\s]+");
    private static final Pattern AREA_CODE_PATTERN = Pattern.compile("^([A-Za-z]+)(0*)(\\d+)$");

    private final UserMapper userMapper;
    private final ItemPostMapper itemPostMapper;
    private final ItemKeywordMapper itemKeywordMapper;
    private final MatchResultMapper matchResultMapper;
    private final ItemPostService itemPostService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${wx.token}")
    private String wxToken;

    @Value("${wx.appid}")
    private String wxAppid;

    @Value("${wx.secret}")
    private String wxSecret;

    @Value("${upload.path:uploads/}")
    private String uploadPath;

    public WxServiceImpl(
            UserMapper userMapper,
            ItemPostMapper itemPostMapper,
            ItemKeywordMapper itemKeywordMapper,
            MatchResultMapper matchResultMapper,
            ItemPostService itemPostService,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.itemPostMapper = itemPostMapper;
        this.itemKeywordMapper = itemKeywordMapper;
        this.matchResultMapper = matchResultMapper;
        this.itemPostService = itemPostService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
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
        Map<String, Object> session = getSession(openid);

        if ("event".equals(msgType) && "subscribe".equals(event)) {
            return handleSubscribe(openid, toUserName);
        }
        if ("image".equals(msgType)
                && session != null
                && FLOW_ADMIN_POST.equals(session.get(FLOW_KEY))
                && STEP_ADMIN_IMAGES.equals(session.get(STEP_KEY))) {
            return handleAdminImage(openid, toUserName, bodyMap, session);
        }
        if ("text".equals(msgType)) {
            String input = content == null ? "" : content.trim();
            if ("取消".equals(input) || "取消发布".equals(input) || "取消全站找帖".equals(input) || "取消查找".equals(input) || "取消绑定".equals(input)) {
                if (session != null) {
                    clearSession(openid);
                    return buildTextReply(openid, toUserName, "已取消当前操作。");
                }
                return buildTextReply(openid, toUserName, defaultGuideText());
            }
            if ("绑定".equals(input)) {
                if (session != null) {
                    clearSession(openid);
                }
                return startBindFlow(openid, toUserName);
            }
            if ("发布".equals(input)) {
                return startAdminPublish(openid, toUserName);
            }
            if ("全站找帖".equals(input) || "查找".equals(input) || "搜索".equals(input)) {
                return startSearchFlow(openid, toUserName);
            }
            if (session != null) {
                String flow = String.valueOf(session.get(FLOW_KEY));
                if (FLOW_ADMIN_POST.equals(flow)) {
                    return handleAdminText(openid, toUserName, input, session);
                }
                if (FLOW_SEARCH.equals(flow)) {
                    return handleSearchText(openid, toUserName, input, session);
                }
                if (FLOW_BIND.equals(flow)) {
                    return handleBindText(openid, toUserName, input, session);
                }
            }
            if ("我的匹配".equals(input) || "查询".equals(input) || "匹配".equals(input)) {
                return handleQuery(openid, toUserName);
            }
            if ("我的帖子".equals(input)) {
                return handleMyPosts(openid, toUserName);
            }
        }
        return buildTextReply(openid, toUserName, defaultGuideText());
    }

    private String handleSubscribe(String openid, String toUserName) {
        String welcomeText = "欢迎关注BISTU失物招领处！\n\n支持以下指令：\n"
                + "【绑定】关联您在网站注册的账号\n"
                + "【我的匹配】查看您的最新匹配结果\n"
                + "【我的帖子】查看您发布的帖子\n"
                + "【全站找帖】按关键词/区域/时间查相似帖子\n"
                + "【发布】管理员快速发布帖子";
        return buildTextReply(openid, toUserName, welcomeText);
    }

    private String startBindFlow(String openid, String toUserName) {
        User user = findUserByOpenid(openid);
        if (user != null && StringUtils.hasText(user.getUsername())) {
            return buildTextReply(
                    openid,
                    toUserName,
                    "您的微信已绑定网站账号：" + user.getUsername() + "。如需更换账号请联系管理员。");
        }
        Map<String, Object> session = new HashMap<>();
        session.put(FLOW_KEY, FLOW_BIND);
        session.put(STEP_KEY, STEP_BIND_IDENTIFIER);
        session.put(DATA_KEY, new HashMap<String, Object>());
        saveSession(openid, session);
        return buildTextReply(
                openid,
                toUserName,
                "已进入账号绑定流程。\n第1步：请输入网站用户名或手机号。");
    }

    private String handleBindText(String openid, String toUserName, String input, Map<String, Object> session) {
        String step = String.valueOf(session.get(STEP_KEY));
        Map<String, Object> data = getData(session);
        switch (step) {
            case STEP_BIND_IDENTIFIER -> {
                if (!StringUtils.hasText(input)) {
                    return buildTextReply(openid, toUserName, "用户名或手机号不能为空，请重新输入。");
                }
                data.put("identifier", input.trim());
                session.put(STEP_KEY, STEP_BIND_PASSWORD);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第2步：请输入网站登录密码。");
            }
            case STEP_BIND_PASSWORD -> {
                String identifier = (String) data.get("identifier");
                clearSession(openid);
                return bindWechatAccount(openid, toUserName, identifier, input);
            }
            default -> {
                clearSession(openid);
                return buildTextReply(openid, toUserName, "绑定流程状态异常，已重置。请重新发送【绑定】。");
            }
        }
    }

    private String bindWechatAccount(String openid, String toUserName, String identifier, String password) {
        if (!StringUtils.hasText(identifier)) {
            return buildTextReply(openid, toUserName, "绑定信息已失效，请重新发送【绑定】。");
        }
        if (!StringUtils.hasText(password)) {
            return buildTextReply(openid, toUserName, "密码不能为空，请重新发送【绑定】。");
        }

        User account = findAccountByIdentifier(identifier.trim());
        if (account == null) {
            return buildTextReply(openid, toUserName, "未找到该网站账号，请确认用户名或手机号后重试。");
        }
        if (!StringUtils.hasText(account.getPasswordHash())
                || !passwordEncoder.matches(password, account.getPasswordHash())) {
            return buildTextReply(openid, toUserName, "密码错误，请重新发送【绑定】。");
        }
        if (StringUtils.hasText(account.getWxOpenid()) && !openid.equals(account.getWxOpenid())) {
            return buildTextReply(openid, toUserName, "该网站账号已绑定其他微信号。");
        }
        if (openid.equals(account.getWxOpenid())) {
            return buildTextReply(openid, toUserName, "您的微信已绑定网站账号：" + account.getUsername() + "。");
        }

        User existingBinding = findUserByOpenid(openid);
        if (existingBinding != null && !existingBinding.getId().equals(account.getId())) {
            if (!isWechatShellUser(existingBinding)) {
                String boundName = StringUtils.hasText(existingBinding.getUsername())
                        ? existingBinding.getUsername()
                        : existingBinding.getPhone();
                return buildTextReply(openid, toUserName, "该微信号已绑定网站账号：" + boundName + "。如需更换请联系管理员。");
            }
            userMapper.update(
                    null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getId, existingBinding.getId())
                            .set(User::getWxOpenid, null));
        }
        userMapper.updateById(User.builder().id(account.getId()).wxOpenid(openid).build());

        String accountName = StringUtils.hasText(account.getUsername()) ? account.getUsername() : account.getPhone();
        return buildTextReply(
                openid,
                toUserName,
                "绑定成功！已关联网站账号：" + accountName + "。\n现在可使用【我的匹配】【我的帖子】等功能。");
    }

    private User findAccountByIdentifier(String identifier) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, identifier)
                .or()
                .eq(User::getPhone, identifier)
                .last("LIMIT 1"));
    }

    private boolean isWechatShellUser(User user) {
        return user != null && !StringUtils.hasText(user.getUsername()) && !StringUtils.hasText(user.getPhone());
    }

    private String shellUserHint() {
        return "您尚未绑定网站账号。请发送【绑定】，输入在网站注册的用户名/手机号与密码，即可查看您在网站上的帖子和匹配记录。";
    }

    private String handleQuery(String openid, String toUserName) {
        User user = findUserByOpenid(openid);
        if (user == null) {
            return buildTextReply(openid, toUserName, shellUserHint());
        }
        if (isWechatShellUser(user)) {
            return buildTextReply(openid, toUserName, shellUserHint());
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
            return buildTextReply(openid, toUserName, shellUserHint());
        }
        if (isWechatShellUser(user)) {
            return buildTextReply(openid, toUserName, shellUserHint());
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
                + "【绑定】关联您在网站注册的账号\n"
                + "【我的匹配】查看您的最新匹配结果\n"
                + "【我的帖子】查看您发布的帖子\n"
                + "【全站找帖】按关键词/区域/时间查相似帖子\n"
                + "【发布】管理员快速发布帖子\n\n"
                + "多轮操作中可发送【取消】退出。\n"
                + "如需更多功能，请访问网站";
    }

    private String startAdminPublish(String openid, String toUserName) {
        User user = findUserByOpenid(openid);
        if (user == null || isWechatShellUser(user)) {
            return buildTextReply(openid, toUserName, shellUserHint());
        }
        if (user.getRole() == null || !user.getRole().canAccessAdminPanel()) {
            return buildTextReply(openid, toUserName, "仅管理员可使用公众号快速发布功能。");
        }
        Map<String, Object> session = new HashMap<>();
        session.put(FLOW_KEY, FLOW_ADMIN_POST);
        session.put(STEP_KEY, STEP_ADMIN_TYPE);
        session.put(DATA_KEY, new HashMap<String, Object>());
        saveSession(openid, session);
        return buildTextReply(openid, toUserName, "已进入管理员快速发布流程。\n第1步：请输入帖子类型【失物】或【寻物】。");
    }

    private String handleAdminText(String openid, String toUserName, String input, Map<String, Object> session) {
        String step = String.valueOf(session.get(STEP_KEY));
        Map<String, Object> data = getData(session);
        switch (step) {
            case STEP_ADMIN_TYPE -> {
                PostType postType = parsePostType(input);
                if (postType == null) {
                    return buildTextReply(openid, toUserName, "帖子类型无效，请输入【失物】或【寻物】。");
                }
                data.put("postType", postType.name());
                session.put(STEP_KEY, STEP_ADMIN_TITLE);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第2步：请输入帖子标题（不超过128字）。");
            }
            case STEP_ADMIN_TITLE -> {
                if (!StringUtils.hasText(input) || input.length() > 128) {
                    return buildTextReply(openid, toUserName, "标题不能为空且长度不能超过128字，请重新输入。");
                }
                data.put("title", input);
                session.put(STEP_KEY, STEP_ADMIN_CATEGORY);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第3步：请输入分类（证件/数码/钥匙/衣物/书籍/其他）。");
            }
            case STEP_ADMIN_CATEGORY -> {
                ItemCategory category = parseCategory(input);
                if (category == null) {
                    return buildTextReply(openid, toUserName, "分类无效，请输入：证件/数码/钥匙/衣物/书籍/其他。");
                }
                data.put("category", category.name());
                session.put(STEP_KEY, STEP_ADMIN_AREA_CODE);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第4步：请输入区域编码（例如 A01）。");
            }
            case STEP_ADMIN_AREA_CODE -> {
                if (!StringUtils.hasText(input)) {
                    return buildTextReply(openid, toUserName, "区域编码不能为空，请重新输入。");
                }
                data.put("areaCode", input);
                session.put(STEP_KEY, STEP_ADMIN_AREA_TEXT);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第5步：请输入区域名称（例如 图书馆）。");
            }
            case STEP_ADMIN_AREA_TEXT -> {
                if (!StringUtils.hasText(input)) {
                    return buildTextReply(openid, toUserName, "区域名称不能为空，请重新输入。");
                }
                data.put("areaText", input);
                session.put(STEP_KEY, STEP_ADMIN_TIME);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第6步：请输入遗失/拾获时间，格式：yyyy-MM-dd HH:mm（例如 2026-05-13 20:30）。");
            }
            case STEP_ADMIN_TIME -> {
                LocalDateTime lostFoundTime = parseDateTime(input);
                if (lostFoundTime == null) {
                    return buildTextReply(openid, toUserName, "时间格式无效，请按 yyyy-MM-dd HH:mm 输入。");
                }
                if (lostFoundTime.isAfter(LocalDateTime.now())) {
                    return buildTextReply(openid, toUserName, "时间不能是未来，请重新输入。");
                }
                data.put("lostFoundTime", lostFoundTime.toString());
                session.put(STEP_KEY, STEP_ADMIN_DESC);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第7步：请输入详细说明（建议包含物品特征）。");
            }
            case STEP_ADMIN_DESC -> {
                if (!StringUtils.hasText(input)) {
                    return buildTextReply(openid, toUserName, "说明不能为空，请重新输入。");
                }
                data.put("description", input);
                session.put(STEP_KEY, STEP_ADMIN_LOCATION);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第8步：请输入详细位置描述（例如 图书馆二层阅览区）。");
            }
            case STEP_ADMIN_LOCATION -> {
                if (!StringUtils.hasText(input)) {
                    return buildTextReply(openid, toUserName, "详细位置不能为空，请重新输入。");
                }
                data.put("locationText", input);
                session.put(STEP_KEY, STEP_ADMIN_CONTACT);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第9步：请输入联系方式（可输入【跳过】）。");
            }
            case STEP_ADMIN_CONTACT -> {
                if (!"跳过".equals(input)) {
                    data.put("contactInfo", input);
                }
                session.put(STEP_KEY, STEP_ADMIN_KEYWORDS);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第10步：请输入关键词（多个用空格或逗号分隔，可输入【跳过】）。");
            }
            case STEP_ADMIN_KEYWORDS -> {
                if (!"跳过".equals(input)) {
                    List<String> keywords = parseKeywords(input);
                    if (keywords.size() > 10) {
                        return buildTextReply(openid, toUserName, "关键词最多10个，请精简后重试。");
                    }
                    data.put("keywords", keywords);
                }
                session.put(STEP_KEY, STEP_ADMIN_IMAGES);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第11步：可发送图片（最多5张，逐张发送）。发送【完成】立即发布，或发送【跳过】不上传图片。");
            }
            case STEP_ADMIN_IMAGES -> {
                if ("完成".equals(input) || "跳过".equals(input)) {
                    return publishAdminPost(openid, toUserName, data);
                }
                return buildTextReply(openid, toUserName, "当前处于图片上传步骤：请发送图片，或发送【完成】发布。");
            }
            default -> {
                clearSession(openid);
                return buildTextReply(openid, toUserName, "流程状态已重置，请重新发送【发布】开始。");
            }
        }
    }

    private String handleAdminImage(String openid, String toUserName, Map<String, String> bodyMap, Map<String, Object> session) {
        Map<String, Object> data = getData(session);
        List<String> imageUrls = getImageUrls(data);
        if (imageUrls.size() >= 5) {
            return buildTextReply(openid, toUserName, "图片已达到5张上限。可继续发送【完成】发布。");
        }
        String mediaId = bodyMap.get("MediaId");
        String picUrl = bodyMap.get("PicUrl");
        try {
            String imageUrl = downloadWechatImage(mediaId);
            imageUrls.add(imageUrl);
            data.put("imageUrls", imageUrls);
            saveSession(openid, session);
            return buildTextReply(openid, toUserName, "已接收第" + imageUrls.size() + "张图片。继续发图，或发送【完成】发布。");
        } catch (Exception ex) {
            if (StringUtils.hasText(picUrl)) {
                imageUrls.add(picUrl);
                data.put("imageUrls", imageUrls);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "图片已暂存（临时链接）。当前共" + imageUrls.size() + "张，发送【完成】发布。");
            }
            return buildTextReply(openid, toUserName, "图片处理失败，请重发图片或发送【完成】继续。");
        }
    }

    private String publishAdminPost(String openid, String toUserName, Map<String, Object> data) {
        User admin = findUserByOpenid(openid);
        if (admin == null || admin.getRole() == null || !admin.getRole().canAccessAdminPanel()) {
            clearSession(openid);
            return buildTextReply(openid, toUserName, "管理员身份验证失败，流程已取消。");
        }
        CreatePostRequest request = new CreatePostRequest();
        request.setPostType(PostType.valueOf(String.valueOf(data.get("postType"))));
        request.setTitle(String.valueOf(data.get("title")));
        request.setCategory(ItemCategory.valueOf(String.valueOf(data.get("category"))));
        request.setDescription((String) data.get("description"));
        request.setLostFoundTime(LocalDateTime.parse(String.valueOf(data.get("lostFoundTime"))));
        request.setAreaCode((String) data.get("areaCode"));
        request.setAreaText((String) data.get("areaText"));
        request.setLocationText((String) data.get("locationText"));
        request.setContactInfo((String) data.get("contactInfo"));
        request.setKeywords(castStringList(data.get("keywords")));
        request.setImageUrls(castStringList(data.get("imageUrls")));
        Long postId = itemPostService.createPost(request, admin.getId());
        clearSession(openid);
        return buildTextReply(openid, toUserName, "发布成功，帖子ID：" + postId + "。\n可在网站查看：/posts/" + postId);
    }

    private String startSearchFlow(String openid, String toUserName) {
        Map<String, Object> session = new HashMap<>();
        session.put(FLOW_KEY, FLOW_SEARCH);
        session.put(STEP_KEY, STEP_SEARCH_KEYWORDS);
        session.put(DATA_KEY, new HashMap<String, Object>());
        saveSession(openid, session);
        return buildTextReply(openid, toUserName, "已进入全站找帖。\n第1步：请输入关键词（多个用空格或逗号分隔）。");
    }

    private String handleSearchText(String openid, String toUserName, String input, Map<String, Object> session) {
        String step = String.valueOf(session.get(STEP_KEY));
        Map<String, Object> data = getData(session);
        switch (step) {
            case STEP_SEARCH_KEYWORDS -> {
                List<String> keywords = parseKeywords(input);
                if (keywords.isEmpty()) {
                    return buildTextReply(openid, toUserName, "关键词不能为空，请重新输入。");
                }
                data.put("keywords", keywords);
                session.put(STEP_KEY, STEP_SEARCH_AREA);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第2步：请输入区域编码（如 A01），或发送【跳过】。");
            }
            case STEP_SEARCH_AREA -> {
                if (!"跳过".equals(input)) {
                    data.put("areaCode", input);
                }
                session.put(STEP_KEY, STEP_SEARCH_TIME);
                saveSession(openid, session);
                return buildTextReply(openid, toUserName, "第3步：请输入时间（yyyy-MM-dd、yyyy-MM-dd HH:mm 或 yyyy-MM-dd HH:mm:ss），或发送【跳过】。");
            }
            case STEP_SEARCH_TIME -> {
                LocalDateTime targetTime = null;
                if (!"跳过".equals(input)) {
                    targetTime = parseDateOrDateTime(input);
                    if (targetTime == null) {
                        return buildTextReply(
                                openid,
                                toUserName,
                                "时间格式无效，请输入 yyyy-MM-dd、yyyy-MM-dd HH:mm 或 yyyy-MM-dd HH:mm:ss，或发送【跳过】。");
                    }
                    data.put("targetTime", targetTime.toString());
                }
                List<SearchResult> results = searchTopMatches(data);
                clearSession(openid);
                return buildTextReply(openid, toUserName, formatSearchReply(results));
            }
            default -> {
                clearSession(openid);
                return buildTextReply(openid, toUserName, "全站找帖流程状态异常，已重置。请重新发送【全站找帖】。");
            }
        }
    }

    private List<SearchResult> searchTopMatches(Map<String, Object> data) {
        List<String> keywords = castStringList(data.get("keywords"));
        if (keywords.isEmpty()) {
            return List.of();
        }
        String areaCode = (String) data.get("areaCode");
        LocalDateTime targetTime = null;
        if (data.get("targetTime") != null) {
            targetTime = LocalDateTime.parse(String.valueOf(data.get("targetTime")));
        }

        List<ItemPost> openPosts = itemPostMapper.selectList(
                new LambdaQueryWrapper<ItemPost>()
                        .in(ItemPost::getStatus, PostStatus.OPEN, PostStatus.MATCHED)
                        .orderByDesc(ItemPost::getCreatedAt)
                        .last("LIMIT 200"));

        List<SearchResult> candidates = new ArrayList<>();
        for (ItemPost post : openPosts) {
            Score score = calculateSearchScore(post, keywords, areaCode, targetTime);
            if (score.total.compareTo(BigDecimal.valueOf(0.20D)) < 0) {
                continue;
            }
            candidates.add(new SearchResult(post, score.total));
        }
        candidates.sort(Comparator.comparing(SearchResult::score).reversed());
        return candidates.size() > 5 ? candidates.subList(0, 5) : candidates;
    }

    private Score calculateSearchScore(ItemPost post, List<String> keywords, String areaCode, LocalDateTime targetTime) {
        String title = String.valueOf(post.getTitle() == null ? "" : post.getTitle());
        String desc = String.valueOf(post.getDescription() == null ? "" : post.getDescription());
        String areaText = String.valueOf(post.getAreaText() == null ? "" : post.getAreaText());
        List<String> tagKeywords = itemKeywordMapper
                .selectList(new LambdaQueryWrapper<ItemKeyword>().eq(ItemKeyword::getPostId, post.getId()))
                .stream()
                .map(ItemKeyword::getKeyword)
                .filter(StringUtils::hasText)
                .toList();

        int hits = 0;
        for (String keyword : keywords) {
            boolean matched = keywordMatches(title, keyword)
                    || keywordMatches(desc, keyword)
                    || keywordMatches(areaText, keyword)
                    || tagKeywords.stream().anyMatch(tag -> keywordMatches(tag, keyword));
            if (matched) {
                hits++;
            }
        }
        BigDecimal keywordScore = BigDecimal.valueOf((double) hits / keywords.size())
                .multiply(BigDecimal.valueOf(0.70D))
                .setScale(4, RoundingMode.HALF_UP);
        if (hits == 0) {
            return new Score(BigDecimal.ZERO);
        }

        BigDecimal areaScore = BigDecimal.ZERO;
        if (StringUtils.hasText(areaCode)) {
            if (StringUtils.hasText(post.getAreaCode()) && areaCodesMatch(areaCode, post.getAreaCode())) {
                areaScore = BigDecimal.valueOf(0.20D);
            } else if (StringUtils.hasText(post.getAreaText()) && keywordMatches(post.getAreaText(), areaCode)) {
                areaScore = BigDecimal.valueOf(0.15D);
            }
        }

        BigDecimal timeScore = BigDecimal.ZERO;
        if (targetTime != null && post.getLostFoundTime() != null) {
            long diffHours = Math.abs(Duration.between(targetTime, post.getLostFoundTime()).toHours());
            if (diffHours <= 24) {
                timeScore = BigDecimal.valueOf(0.10D);
            } else if (diffHours <= 72) {
                timeScore = BigDecimal.valueOf(0.05D);
            }
        }
        return new Score(keywordScore.add(areaScore).add(timeScore).setScale(4, RoundingMode.HALF_UP));
    }

    private boolean keywordMatches(String source, String keyword) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(keyword)) {
            return false;
        }
        String lowerKeyword = keyword.trim().toLowerCase();
        String lowerSource = source.trim().toLowerCase();
        return lowerSource.contains(lowerKeyword) || lowerKeyword.contains(lowerSource);
    }

    private boolean areaCodesMatch(String left, String right) {
        String normalizedLeft = normalizeAreaCode(left);
        String normalizedRight = normalizeAreaCode(right);
        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return false;
        }
        if (normalizedLeft.equalsIgnoreCase(normalizedRight)) {
            return true;
        }
        return normalizeAreaDigits(normalizedLeft).equalsIgnoreCase(normalizeAreaDigits(normalizedRight));
    }

    private String normalizeAreaCode(String areaCode) {
        return areaCode.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private String normalizeAreaDigits(String areaCode) {
        Matcher matcher = AREA_CODE_PATTERN.matcher(areaCode);
        if (matcher.matches()) {
            return matcher.group(1) + matcher.group(3);
        }
        return areaCode;
    }

    private String formatSearchReply(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "没有找到足够相似的帖子。\n建议更换关键词或放宽时间条件后重试。";
        }
        StringBuilder sb = new StringBuilder("为您找到 ").append(results.size()).append(" 条最相似帖子：\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            ItemPost post = result.post();
            int percent = result.score().multiply(BigDecimal.valueOf(100)).intValue();
            sb.append(i + 1)
                    .append(".【")
                    .append(post.getPostType() == PostType.LOST ? "失物" : "寻物")
                    .append("】")
                    .append(post.getTitle())
                    .append("\n  相似度：")
                    .append(percent)
                    .append("%，区域：")
                    .append(StringUtils.hasText(post.getAreaText()) ? post.getAreaText() : "-")
                    .append("\n  查看：https://www.bistulf.art/posts/")
                    .append(post.getId())
                    .append("\n");
        }
        sb.append("如需继续找帖，可再次发送【全站找帖】。");
        return sb.toString();
    }

    private String downloadWechatImage(String mediaId) throws Exception {
        if (!StringUtils.hasText(mediaId)) {
            throw new IllegalArgumentException("mediaId 为空");
        }
        String token = getWxAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/media/get?access_token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&media_id="
                + URLEncoder.encode(mediaId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
        if (!contentType.startsWith("image/")) {
            throw new IOException("下载媒体失败，返回非图片类型");
        }
        String extension = contentType.contains("png") ? "png" : contentType.contains("webp") ? "webp" : "jpg";
        String monthDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String generatedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String rootPath = StringUtils.trimTrailingCharacter(uploadPath, '/');
        Path targetDir = Paths.get(rootPath, monthDir);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Path targetFile = targetDir.resolve(generatedName);
        Files.write(targetFile, response.body());
        return "/" + rootPath + "/" + monthDir + "/" + generatedName;
    }

    private String getWxAccessToken() throws Exception {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + URLEncoder.encode(wxAppid, StandardCharsets.UTF_8)
                + "&secret="
                + URLEncoder.encode(wxSecret, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode token = root.get("access_token");
        if (token == null || !StringUtils.hasText(token.asText())) {
            throw new IOException("获取 access_token 失败");
        }
        return token.asText();
    }

    private PostType parsePostType(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String value = input.trim().toUpperCase();
        if ("失物".equals(input) || "LOST".equals(value)) {
            return PostType.LOST;
        }
        if ("寻物".equals(input) || "FOUND".equals(value)) {
            return PostType.FOUND;
        }
        return null;
    }

    private ItemCategory parseCategory(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String value = input.trim().toUpperCase();
        for (ItemCategory category : ItemCategory.values()) {
            if (category.name().equals(value) || category.getValue().equals(value) || category.getDesc().equals(input.trim())) {
                return category;
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateOrDateTime(String input) {
        String normalized = input.trim();
        try {
            return LocalDateTime.parse(normalized, DATETIME_SECONDS_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return LocalDateTime.parse(normalized, DATETIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(normalized, DATE_FORMATTER).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private List<String> parseKeywords(String input) {
        if (!StringUtils.hasText(input)) {
            return List.of();
        }
        return SPLIT_PATTERN.splitAsStream(input.trim())
                .filter(StringUtils::hasText)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getData(Map<String, Object> session) {
        Object data = session.get(DATA_KEY);
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> newData = new HashMap<>();
        session.put(DATA_KEY, newData);
        return newData;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(item -> item instanceof String text ? text : String.valueOf(item))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private List<String> getImageUrls(Map<String, Object> data) {
        List<String> imageUrls = castStringList(data.get("imageUrls"));
        if (imageUrls.isEmpty()) {
            imageUrls = new ArrayList<>();
        } else {
            imageUrls = new ArrayList<>(imageUrls);
        }
        return imageUrls;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSession(String openid) {
        if (!StringUtils.hasText(openid)) {
            return null;
        }
        Object raw = redisTemplate.opsForValue().get(buildSessionKey(openid));
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private void saveSession(String openid, Map<String, Object> session) {
        redisTemplate.opsForValue().set(buildSessionKey(openid), session, 15, TimeUnit.MINUTES);
    }

    private void clearSession(String openid) {
        redisTemplate.delete(buildSessionKey(openid));
    }

    private String buildSessionKey(String openid) {
        return "wx:session:" + openid;
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

    private record Score(BigDecimal total) {}

    private record SearchResult(ItemPost post, BigDecimal score) {}
}

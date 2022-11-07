package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public Result getBlogById(Long id) {

        Blog blog = query().eq("id", id).one();
        User user = userService.query().eq("id", blog.getUserId()).one();
        getLiked(blog);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        String key = "Blog:like:"+id;

        //1.查询redis点赞列表是否存在该用户
        Double score = redisTemplate.opsForZSet().score(key, user.getId());
        //2.1如果存在
        //2.11 取消点赞
        if (score != null){
            //2.12 redis set集合删除用户，修改数据库点赞数量
            redisTemplate.opsForZSet().remove(key,user.getId());
            update().setSql("liked = liked-1").eq("id",id).update();
            return Result.ok();
        }
        //2.2如果不存在
        //2.21 redis set集合增加用户，修改数据库点赞数量
        redisTemplate.opsForZSet().add(key,user.getId(),System.currentTimeMillis());
        update().setSql("liked = liked+1").eq("id",id).update();
        return Result.ok();
    }
    public void getLiked(Blog blog){
        String key = "Blog:like:"+blog.getId();
        UserDTO user = UserHolder.getUser();
        if (ObjectUtils.isNotEmpty(user)){
            Double score = redisTemplate.opsForZSet().score(key, user.getId());
            blog.setIsLike(score != null);
        }

    }

    @Override
    public Result queryBlogList(Integer current) {
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            getLiked(blog);
            blog.setIcon(user.getIcon());
        });
        return Result.ok(records);
    }

    @Override
    public Result getLikeRange(Long id) {
        String key = "Blog:like:"+id;

        Set<Long> range = redisTemplate.opsForZSet().range(key, 0, 4);
        List<Long> ids = new ArrayList<>(range);
        List<UserDTO> users = userService
                .listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        System.out.println(users);
        return Result.ok(users);
    }
}

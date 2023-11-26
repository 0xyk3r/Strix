package cn.projectan.strix.service;

import cn.projectan.strix.core.exception.StrixJobException;
import cn.projectan.strix.model.db.Job;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quartz.SchedulerException;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-07-30
 */
public interface JobService extends IService<Job> {

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    boolean pauseJob(Job job) throws SchedulerException;

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    boolean resumeJob(Job job) throws SchedulerException;

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     * @return 结果
     */
    boolean deleteJob(Job job) throws SchedulerException;

    /**
     * 批量删除调度信息
     *
     * @param jobIds 需要删除的任务ID
     */
    void deleteJobByIds(String[] jobIds) throws SchedulerException;

    /**
     * 任务调度状态修改
     *
     * @param job 调度信息
     * @return 结果
     */
    boolean changeStatus(Job job) throws SchedulerException;

    /**
     * 立即运行任务
     *
     * @param id 调度ID
     * @return 结果
     */
    boolean run(String id) throws SchedulerException;

    /**
     * 新增任务
     *
     * @param job 调度信息
     */
    void insertJob(Job job) throws SchedulerException, StrixJobException;

    /**
     * 更新任务
     *
     * @param job 调度信息
     */
    void updateJob(Job job) throws SchedulerException, StrixJobException;

    /**
     * 检查任务信息是否合法，不合法则抛出异常
     *
     * @param job 调度信息
     */
    void checkJobLegal(Job job);

}

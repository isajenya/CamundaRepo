package com.camunda.training;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {

    @Test
    @Deployment(resources = "twitter-qa.bpmn")
    public void testHappyPath() {

        Map<String, Object> variables = new HashMap<String, Object>();
        Random random = new Random();
        variables.put("content", "EX4_IS_" + random.nextInt());
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

        assertThat(processInstance).isWaitingAt("Activity_19uspxt");
        List<Task> taskList = taskService()
                .createTaskQuery()
                .taskCandidateGroup("management")
                .processInstanceId(processInstance.getId())
                .list();

        assertThat(taskList).isNotNull();
        assertThat(taskList).hasSize(1);

        Task task = taskList.get(0);

        Map<String, Object> approvedMap = new HashMap<String, Object>();
        approvedMap.put("approved", true);
        taskService().complete(task.getId(), approvedMap);


        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();
        assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);
        execute(job);

        assertThat(processInstance).isNotNull();
        assertThat(processInstance).isEnded();
    }


    @Test
    @Deployment(resources = "twitter-qa.bpmn")
    public void testTweetRejected() {
        Map<String, Object> varMap = new HashMap<String, Object>();
        Random random = new Random();
        varMap.put("content", "EX8_IS_" + random.nextInt());
        varMap.put("approved", false);

        ProcessInstance processInstance = runtimeService().
                createProcessInstanceByKey("TwitterQAProcess")
                .setVariables(varMap)
                .startAfterActivity("review_tweet")
                .execute();

        assertThat(processInstance).isEnded().hasPassed(findId("tweet_rejected"));


    }

}

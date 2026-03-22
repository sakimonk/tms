package com.test.tms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.test.tms.constants.TodoBlockedFilter;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import com.test.tms.model.dto.TodoResponse;
import com.test.tms.service.TodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tms.testsupport.ControllerTestApplication;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ControllerTestApplication.class)
@AutoConfigureMockMvc
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    private static TodoResponse sampleTodoResponse() {
        TodoResponse r = new TodoResponse();
        r.setId(1L);
        r.setUserId(10L);
        r.setName("单元测试任务");
        r.setDescription("desc");
        r.setDueDate(LocalDateTime.of(2026, 6, 15, 9, 0));
        r.setStatus(TodoStatus.NOT_STARTED);
        r.setPriority(TodoPriority.HIGH);
        r.setDependsOnTodoIds(List.of());
        return r;
    }

    @Nested
    @DisplayName("POST /todos")
    class CreateTodo {

        @Test
        @DisplayName("合法请求体返回 200 且响应体为 TodoResponse")
        void createReturns200() throws Exception {
            TodoResponse body = sampleTodoResponse();
            when(todoService.createTodo(any())).thenReturn(body);

            String json = """
                    {
                      "userId": 10,
                      "name": "单元测试任务",
                      "description": "desc",
                      "dueDate": "2026-06-15T09:00:00",
                      "status": "NOT_STARTED",
                      "priority": "HIGH",
                      "isRecurring": false,
                      "dependsOnTodoIds": []
                    }
                    """;

            mockMvc.perform(post("/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.userId").value(10))
                    .andExpect(jsonPath("$.name").value("单元测试任务"))
                    .andExpect(jsonPath("$.status").value("NOT_STARTED"));

            verify(todoService).createTodo(any());
        }

        @Test
        @DisplayName("缺少必填字段时返回 400")
        void createReturns400WhenInvalid() throws Exception {
            String json = """
                    {
                      "name": "only name"
                    }
                    """;

            mockMvc.perform(post("/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
        }
    }

    @Nested
    @DisplayName("GET /todos/{id}")
    class GetById {

        @Test
        @DisplayName("返回 200 与详情")
        void getByIdReturns200() throws Exception {
            when(todoService.getTodo(5L)).thenReturn(sampleTodoResponse());

            mockMvc.perform(get("/todos/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(todoService).getTodo(5L);
        }
    }

    @Nested
    @DisplayName("GET /todos")
    class ListTodos {

        @Test
        @DisplayName("分页与筛选参数会传给 Service")
        void listPassesQueryParams() throws Exception {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

            Page<TodoResponse> page = new Page<>(2, 10);
            page.setRecords(List.of(sampleTodoResponse()));
            page.setTotal(1L);
            when(todoService.listTodos(
                    eq(2L),
                    eq(10L),
                    eq(TodoStatus.IN_PROGRESS),
                    eq(TodoPriority.MEDIUM),
                    eq(from),
                    eq(to),
                    eq(TodoBlockedFilter.BLOCKED),
                    eq("due_date"),
                    eq("desc")
            )).thenReturn(page);

            mockMvc.perform(get("/todos")
                            .param("pageNum", "2")
                            .param("pageSize", "10")
                            .param("status", "IN_PROGRESS")
                            .param("priority", "MEDIUM")
                            .param("dueFrom", "2026-01-01T00:00:00")
                            .param("dueTo", "2026-12-31T23:59:59")
                            .param("blockedFilter", "BLOCKED")
                            .param("sortBy", "due_date")
                            .param("sortDir", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.records").isArray())
                    .andExpect(jsonPath("$.records[0].name").value("单元测试任务"));

            verify(todoService).listTodos(
                    eq(2L),
                    eq(10L),
                    eq(TodoStatus.IN_PROGRESS),
                    eq(TodoPriority.MEDIUM),
                    eq(from),
                    eq(to),
                    eq(TodoBlockedFilter.BLOCKED),
                    eq("due_date"),
                    eq("desc")
            );
        }

        @Test
        @DisplayName("默认分页参数为 pageNum=1, pageSize=20")
        void listDefaultPagination() throws Exception {
            Page<TodoResponse> page = new Page<>(1, 20);
            page.setRecords(List.of());
            page.setTotal(0L);

            when(todoService.listTodos(
                    eq(1L),
                    eq(20L),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq("due_date"),
                    eq("asc")
            )).thenReturn(page);

            mockMvc.perform(get("/todos"))
                    .andExpect(status().isOk());

            verify(todoService).listTodos(
                    eq(1L),
                    eq(20L),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq("due_date"),
                    eq("asc")
            );
        }
    }

    @Nested
    @DisplayName("PATCH /todos/batch/status")
    class BatchStatus {

        @Test
        @DisplayName("合法请求返回 204")
        void batchReturns204() throws Exception {
            doNothing().when(todoService).batchUpdateTodoStatus(any());

            String json = """
                    {
                      "ids": [1, 2, 3],
                      "status": "COMPLETED",
                      "updatedBy": 99
                    }
                    """;

            mockMvc.perform(patch("/todos/batch/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent());

            verify(todoService).batchUpdateTodoStatus(any());
        }

        @Test
        @DisplayName("ids 为空时返回 400")
        void batchReturns400WhenIdsEmpty() throws Exception {
            String json = """
                    {
                      "ids": [],
                      "status": "COMPLETED"
                    }
                    """;

            mockMvc.perform(patch("/todos/batch/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /todos/{id}")
    class UpdateTodo {

        @Test
        @DisplayName("合法请求返回 204")
        void updateReturns204() throws Exception {
            doNothing().when(todoService).updateTodo(anyLong(), any());

            String json = """
                    {
                      "name": "新标题",
                      "status": "IN_PROGRESS"
                    }
                    """;

            mockMvc.perform(put("/todos/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent());

            verify(todoService).updateTodo(eq(7L), any());
        }
    }

    @Nested
    @DisplayName("DELETE /todos/{id}")
    class DeleteTodo {

        @Test
        @DisplayName("默认 deleteAssociated=true 返回 204")
        void deleteReturns204() throws Exception {
            doNothing().when(todoService).softDeleteTodo(anyLong(), any(), anyBoolean());

            mockMvc.perform(delete("/todos/3"))
                    .andExpect(status().isNoContent());

            verify(todoService).softDeleteTodo(eq(3L), isNull(), eq(true));
        }

        @Test
        @DisplayName("可传 updatedBy 与 deleteAssociated=false")
        void deleteWithQueryParams() throws Exception {
            doNothing().when(todoService).softDeleteTodo(anyLong(), any(), anyBoolean());

            mockMvc.perform(delete("/todos/3")
                            .param("updatedBy", "100")
                            .param("deleteAssociated", "false"))
                    .andExpect(status().isNoContent());

            verify(todoService).softDeleteTodo(eq(3L), eq(100L), eq(false));
        }
    }
}

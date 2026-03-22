(function () {
  'use strict';

  var API = '/todos';
  var LS_USER = 'tms.ui.userId';

  var state = {
    pageNum: 1,
    pageSize: 20,
    totalPages: 1,
    filters: {
      status: '',
      priority: '',
      blockedFilter: '',
      sortBy: 'due_date',
      sortDir: 'asc',
    },
  };

  function $(id) {
    return document.getElementById(id);
  }

  function toast(msg, isError) {
    var el = $('toast');
    el.textContent = msg;
    el.classList.remove('hidden');
    el.classList.toggle('error', !!isError);
    clearTimeout(toast._t);
    toast._t = setTimeout(function () {
      el.classList.add('hidden');
    }, 5000);
  }

  function parseDependsOnTodoIds(str) {
    if (str == null || String(str).trim() === '') {
      return [];
    }
    return String(str)
      .split(/[,，\s]+/)
      .map(function (s) {
        return s.trim();
      })
      .filter(function (s) {
        return s.length > 0;
      })
      .map(function (s) {
        return parseInt(s, 10);
      })
      .filter(function (n) {
        return !isNaN(n) && n >= 1;
      });
  }

  function toDatetimeLocalValue(iso) {
    if (!iso) {
      return '';
    }
    var s = String(iso);
    if (s.length >= 16) {
      return s.slice(0, 16);
    }
    return s;
  }

  function fromDatetimeLocal(v) {
    if (!v) {
      return null;
    }
    if (v.length === 16) {
      return v + ':00';
    }
    return v;
  }

  function getUserId() {
    var v = $('userId').value.trim();
    var n = parseInt(v, 10);
    if (isNaN(n) || n < 1) {
      return null;
    }
    return n;
  }

  function saveUserId() {
    var n = getUserId();
    if (n == null) {
      toast('请输入有效的用户 ID（≥1）', true);
      return;
    }
    localStorage.setItem(LS_USER, String(n));
    toast('已保存用户 ID：' + n);
  }

  function loadUserId() {
    var s = localStorage.getItem(LS_USER);
    if (s) {
      $('userId').value = s;
    }
  }

  async function apiFetch(path, options) {
    var opts = options || {};
    var headers = opts.headers || {};
    if (!headers['Content-Type'] && opts.body && typeof opts.body === 'string') {
      headers['Content-Type'] = 'application/json';
    }
    var res = await fetch(path, {
      method: opts.method || 'GET',
      headers: headers,
      body: opts.body,
    });
    var text = await res.text();
    var data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = text;
      }
    }
    if (!res.ok) {
      var msg =
        typeof data === 'object' && data !== null && data.message
          ? data.message
          : typeof data === 'string'
            ? data
            : res.status + ' ' + res.statusText;
      throw new Error(msg);
    }
    return data;
  }

  function buildQuery() {
    var q = new URLSearchParams();
    q.set('pageNum', String(state.pageNum));
    q.set('pageSize', String(state.pageSize));
    if (state.filters.status) {
      q.set('status', state.filters.status);
    }
    if (state.filters.priority) {
      q.set('priority', state.filters.priority);
    }
    if (state.filters.blockedFilter) {
      q.set('blockedFilter', state.filters.blockedFilter);
    }
    q.set('sortBy', state.filters.sortBy);
    q.set('sortDir', state.filters.sortDir);
    return q.toString();
  }

  async function loadTodos() {
    var url = API + '?' + buildQuery();
    var data = await apiFetch(url);
    var records = (data && data.records) || [];
    var total = data && data.total != null ? data.total : records.length;
    var current = data && data.current != null ? data.current : state.pageNum;
    var size = data && data.size != null ? data.size : state.pageSize;
    var pages = data && data.pages != null ? data.pages : 1;

    state.pageNum = current;
    state.totalPages = pages;

    $('listMeta').textContent =
      '共 ' + total + ' 条 · 第 ' + current + ' / ' + pages + ' 页 · 每页 ' + size + ' 条';

    $('pageInfo').textContent = current + ' / ' + pages;

    var tbody = $('todoTbody');
    tbody.innerHTML = '';
    records.forEach(function (row) {
      var tr = document.createElement('tr');
      var deps = row.dependsOnTodoIds;
      var depStr =
        Array.isArray(deps) && deps.length ? deps.join(', ') : '—';
      tr.innerHTML =
        '<td>' +
        escapeHtml(String(row.id)) +
        '</td>' +
        '<td>' +
        escapeHtml(row.name || '') +
        '</td>' +
        '<td>' +
        escapeHtml(formatDate(row.dueDate)) +
        '</td>' +
        '<td>' +
        escapeHtml(String(row.status || '')) +
        '</td>' +
        '<td>' +
        escapeHtml(String(row.priority || '')) +
        '</td>' +
        '<td>' +
        escapeHtml(String(row.blockingDepCount != null ? row.blockingDepCount : '')) +
        '</td>' +
        '<td class="deps">' +
        escapeHtml(depStr) +
        '</td>' +
        '<td class="actions">' +
        '<button type="button" data-action="edit" data-id="' +
        row.id +
        '">编辑</button> ' +
        '<button type="button" data-action="del" data-id="' +
        row.id +
        '" class="danger">删除</button>' +
        '</td>';
      tbody.appendChild(tr);
    });
  }

  function escapeHtml(s) {
    var d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  function formatDate(v) {
    if (v == null) {
      return '';
    }
    return String(v).replace('T', ' ');
  }

  async function deleteTodo(id) {
    if (!confirm('确认删除 TODO #' + id + '？（软删除）')) {
      return;
    }
    try {
      var uid = getUserId();
      var q = uid != null ? '?updatedBy=' + encodeURIComponent(uid) : '';
      await apiFetch(API + '/' + id + q, { method: 'DELETE' });
      toast('已删除');
      await loadTodos();
    } catch (e) {
      toast(e.message || String(e), true);
    }
  }

  function readFiltersFromDom() {
    state.filters.status = $('filterStatus').value;
    state.filters.priority = $('filterPriority').value;
    state.filters.blockedFilter = $('filterBlocked').value;
    state.filters.sortBy = $('sortBy').value;
    state.filters.sortDir = $('sortDir').value;
    state.pageSize = parseInt($('pageSize').value, 10) || 20;
    state.pageNum = 1;
  }

  function syncFiltersToDom() {
    $('filterStatus').value = state.filters.status;
    $('filterPriority').value = state.filters.priority;
    $('filterBlocked').value = state.filters.blockedFilter;
    $('sortBy').value = state.filters.sortBy;
    $('sortDir').value = state.filters.sortDir;
    $('pageSize').value = String(state.pageSize);
  }

  function toggleCreateRecurrence() {
    var on = $('createIsRecurring').checked;
    $('createRecurrence').classList.toggle('hidden', !on);
  }

  async function onCreateSubmit(ev) {
    ev.preventDefault();
    var uid = getUserId();
    if (uid == null) {
      toast('请先填写并保存用户 ID', true);
      return;
    }
    var fd = new FormData(ev.target);
    var isRecurring = $('createIsRecurring').checked;
    var body = {
      userId: uid,
      name: fd.get('name'),
      description: fd.get('description') || null,
      dueDate: fromDatetimeLocal(fd.get('dueDate')),
      status: fd.get('status'),
      priority: fd.get('priority'),
      isRecurring: isRecurring,
      updatedBy: uid,
      createdBy: uid,
    };
    if (isRecurring) {
      body.recurrenceType = fd.get('recurrenceType');
      body.recurrenceInterval = parseInt(fd.get('recurrenceInterval'), 10) || 1;
      var cron = fd.get('recurrenceCron');
      if (cron) {
        body.recurrenceCron = cron;
      }
    }
    var depStr = fd.get('dependsOnTodoIds');
    var deps = parseDependsOnTodoIds(depStr);
    if (deps.length) {
      body.dependsOnTodoIds = deps;
    }
    try {
      await apiFetch(API, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      toast('创建成功');
      ev.target.reset();
      $('createIsRecurring').checked = false;
      toggleCreateRecurrence();
      await loadTodos();
    } catch (e) {
      toast(e.message || String(e), true);
    }
  }

  async function openEdit(id) {
    try {
      var row = await apiFetch(API + '/' + id);
      var f = $('formEdit');
      $('editTodoId').value = row.id;
      f.name.value = row.name || '';
      f.description.value = row.description || '';
      f.dueDate.value = toDatetimeLocalValue(row.dueDate);
      f.status.value = row.status || 'NOT_STARTED';
      f.priority.value = row.priority || 'MEDIUM';
      var deps = row.dependsOnTodoIds;
      f.dependsOnTodoIds.value =
        Array.isArray(deps) && deps.length ? deps.join(', ') : '';

      var hint = $('editRecurrenceHint');
      if (row.recurrenceId != null) {
        hint.textContent =
          '此任务为循环实例（recurrenceId=' +
          row.recurrenceId +
          '）。循环规则请在接口层单独维护；此处仅改内容、状态与依赖。';
        hint.classList.remove('hidden');
      } else {
        hint.textContent = '';
        hint.classList.add('hidden');
      }

      $('modal').classList.remove('hidden');
    } catch (e) {
      toast(e.message || String(e), true);
    }
  }

  function closeEdit() {
    $('modal').classList.add('hidden');
  }

  async function onEditSubmit(ev) {
    ev.preventDefault();
    var uid = getUserId();
    var f = ev.target;
    var id = $('editTodoId').value;
    var body = {
      name: f.name.value,
      description: f.description.value || null,
      dueDate: fromDatetimeLocal(f.dueDate.value),
      status: f.status.value,
      priority: f.priority.value,
      dependsOnTodoIds: parseDependsOnTodoIds(f.dependsOnTodoIds.value),
    };
    if (uid != null) {
      body.updatedBy = uid;
    }
    try {
      await apiFetch(API + '/' + id, {
        method: 'PUT',
        body: JSON.stringify(body),
      });
      toast('已保存');
      closeEdit();
      await loadTodos();
    } catch (e) {
      toast(e.message || String(e), true);
    }
  }

  function bind() {
    $('btnSaveUser').onclick = saveUserId;
    $('btnApplyFilter').onclick = function () {
      readFiltersFromDom();
      loadTodos().catch(function (e) {
        toast(e.message || String(e), true);
      });
    };
    $('btnRefresh').onclick = function () {
      loadTodos().catch(function (e) {
        toast(e.message || String(e), true);
      });
    };
    $('btnPrev').onclick = function () {
      if (state.pageNum <= 1) {
        return;
      }
      state.pageNum--;
      loadTodos().catch(function (e) {
        toast(e.message || String(e), true);
      });
    };
    $('btnNext').onclick = function () {
      if (state.pageNum >= state.totalPages) {
        return;
      }
      state.pageNum++;
      loadTodos().catch(function (e) {
        state.pageNum--;
        toast(e.message || String(e), true);
      });
    };

    $('todoTbody').addEventListener('click', function (ev) {
      var btn = ev.target.closest('button[data-action]');
      if (!btn) {
        return;
      }
      var id = parseInt(btn.getAttribute('data-id'), 10);
      var action = btn.getAttribute('data-action');
      if (action === 'edit') {
        openEdit(id);
      } else if (action === 'del') {
        deleteTodo(id);
      }
    });
    $('createIsRecurring').onchange = toggleCreateRecurrence;
    $('formCreate').onsubmit = onCreateSubmit;
    $('formEdit').onsubmit = onEditSubmit;
    $('btnCancelEdit').onclick = closeEdit;
    $('modal').onclick = function (ev) {
      if (ev.target === $('modal')) {
        closeEdit();
      }
    };
  }

  function init() {
    loadUserId();
    syncFiltersToDom();
    bind();
    toggleCreateRecurrence();
    loadTodos().catch(function (e) {
      toast(e.message || String(e), true);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

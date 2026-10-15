#!/usr/bin/env bash

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUNTIME_DIR="${PROJECT_ROOT}/.runtime"
PID_DIR="${RUNTIME_DIR}/pids"
LOG_DIR="${RUNTIME_DIR}/logs"

mkdir -p "${PID_DIR}" "${LOG_DIR}"

MINIO_DIR="${MINIO_DIR:-/Users/itwanger/Downloads/minio1}"
MINIO_API_PORT="${MINIO_API_PORT:-9000}"
MINIO_CONSOLE_PORT="${MINIO_CONSOLE_PORT:-9001}"
MINIO_CMD="${MINIO_CMD:-./minio server data/ --console-address :${MINIO_CONSOLE_PORT}}"
MINIO_HEALTH_URL="${MINIO_HEALTH_URL:-http://127.0.0.1:${MINIO_API_PORT}/minio/health/live}"
MINIO_CONSOLE_URL="${MINIO_CONSOLE_URL:-http://127.0.0.1:${MINIO_CONSOLE_PORT}}"
MINIO_PATTERN="${MINIO_PATTERN:-minio server data/}"
MINIO_SESSION_NAME="${MINIO_SESSION_NAME:-paiflow-minio}"

pid_file() {
    echo "${PID_DIR}/minio.pid"
}

log_file() {
    echo "${LOG_DIR}/minio.log"
}

discover_pid() {
    local pid
    pid="$(lsof -tiTCP:"${MINIO_API_PORT}" -sTCP:LISTEN 2>/dev/null | head -n 1)"
    if [ -n "${pid}" ]; then
        echo "${pid}"
        return 0
    fi
    return 1
}

discover_pids() {
    lsof -tiTCP:"${MINIO_API_PORT}" -sTCP:LISTEN 2>/dev/null || true
}

tmux_session_exists() {
    tmux has-session -t "${MINIO_SESSION_NAME}" >/dev/null 2>&1
}

port_is_listening() {
    local port="$1"
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN > /dev/null 2>&1
}

http_ok() {
    local url="$1"
    curl --silent --fail --max-time 2 "${url}" > /dev/null 2>&1
}

health_check() {
    http_ok "${MINIO_HEALTH_URL}" || return 1
    http_ok "${MINIO_CONSOLE_URL}" || return 1
    return 0
}

wait_for_minio() {
    local i
    for i in $(seq 1 30); do
        if health_check; then
            return 0
        fi
        sleep 1
    done
    return 1
}

is_running() {
    if health_check; then
        local discovered_pid
        discovered_pid="$(discover_pid || true)"
        if [ -n "${discovered_pid}" ]; then
            echo "${discovered_pid}" > "$(pid_file)"
        fi
        return 0
    fi

    local pid_path
    pid_path="$(pid_file)"
    if [ ! -f "${pid_path}" ]; then
        return 1
    fi

    local pid
    pid="$(cat "${pid_path}")"
    if [ -n "${pid}" ] && ps -p "${pid}" > /dev/null 2>&1; then
        return 0
    fi

    local discovered_pid
    discovered_pid="$(discover_pid || true)"
    if [ -n "${discovered_pid}" ]; then
        echo "${discovered_pid}" > "${pid_path}"
        return 0
    fi

    rm -f "${pid_path}"
    return 1
}

validate_minio() {
    if [ ! -d "${MINIO_DIR}" ]; then
        echo "minio 目录不存在: ${MINIO_DIR}"
        return 1
    fi
    return 0
}

start_minio() {
    validate_minio || return 1

    if is_running; then
        echo "minio 已在运行，PID: $(cat "$(pid_file)")"
        return 0
    fi

    local log_path pid_path
    log_path="$(log_file)"
    pid_path="$(pid_file)"

    echo "启动 minio..."
    (
        cd "${MINIO_DIR}" || exit 1
        : > "${log_path}"
        if command -v tmux >/dev/null 2>&1; then
            tmux kill-session -t "${MINIO_SESSION_NAME}" >/dev/null 2>&1 || true
            tmux new-session -d -s "${MINIO_SESSION_NAME}" "cd \"${MINIO_DIR}\" && exec ${MINIO_CMD} >> \"${log_path}\" 2>&1"
        else
            nohup ${MINIO_CMD} >> "${log_path}" 2>&1 < /dev/null &
            echo $! > "${pid_path}"
        fi
    )

    if wait_for_minio; then
        local discovered_pid
        discovered_pid="$(discover_pid || true)"
        if [ -n "${discovered_pid}" ]; then
            echo "${discovered_pid}" > "${pid_path}"
        fi
    fi

    if is_running; then
        echo "minio 启动成功，PID: $(cat "${pid_path}")"
        echo "日志: ${log_path}"
        show_urls
        return 0
    fi

    echo "minio 启动失败，请检查日志: ${log_path}"
    print_diagnostics
    return 1
}

stop_minio() {
    local pid_path
    pid_path="$(pid_file)"

    if ! is_running; then
        if command -v tmux >/dev/null 2>&1 && tmux_session_exists; then
            tmux kill-session -t "${MINIO_SESSION_NAME}" >/dev/null 2>&1 || true
        fi
        echo "minio 未运行"
        return 0
    fi

    local pids
    pids="$(discover_pids)"
    if [ -z "${pids}" ]; then
        pids="$(cat "${pid_path}" 2>/dev/null || true)"
    fi

    echo "停止 minio (PID: ${pids//$'\n'/ })..."
    if command -v tmux >/dev/null 2>&1 && tmux_session_exists; then
        tmux kill-session -t "${MINIO_SESSION_NAME}" >/dev/null 2>&1 || true
    fi
    while IFS= read -r current_pid; do
        [ -n "${current_pid}" ] || continue
        kill "${current_pid}" 2>/dev/null || true
    done <<< "${pids}"

    for _ in 1 2 3 4 5; do
        if ! is_running; then
            rm -f "${pid_path}"
            echo "minio 已停止"
            return 0
        fi
        sleep 1
    done

    echo "minio 未在预期时间内退出，执行强制停止"
    while IFS= read -r current_pid; do
        [ -n "${current_pid}" ] || continue
        kill -9 "${current_pid}" 2>/dev/null || true
    done <<< "${pids}"
    rm -f "${pid_path}"
    echo "minio 已强制停止"
}

status_minio() {
    if is_running; then
        local pid
        pid="$(cat "$(pid_file)" 2>/dev/null || true)"
        if command -v tmux >/dev/null 2>&1 && tmux_session_exists; then
            echo "minio: 运行中 (PID: ${pid:-unknown}, API: ${MINIO_API_PORT}, Console: ${MINIO_CONSOLE_PORT}, Session: ${MINIO_SESSION_NAME})"
        else
            echo "minio: 运行中 (PID: ${pid:-unknown}, API: ${MINIO_API_PORT}, Console: ${MINIO_CONSOLE_PORT})"
        fi
    else
        echo "minio: 未运行"
    fi
}

logs_minio() {
    local log_path
    log_path="$(log_file)"
    if [ ! -f "${log_path}" ]; then
        echo "minio 暂无日志: ${log_path}"
        return 1
    fi
    tail -f "${log_path}"
}

show_urls() {
    cat <<EOF
MinIO API:     ${MINIO_HEALTH_URL%/minio/health/live}
MinIO Console: ${MINIO_CONSOLE_URL}
EOF
}

print_diagnostics() {
    echo "健康检查失败: ${MINIO_HEALTH_URL}"

    if port_is_listening "${MINIO_API_PORT}"; then
        echo "端口 ${MINIO_API_PORT} 正在监听"
    else
        echo "端口 ${MINIO_API_PORT} 未监听"
    fi

    if port_is_listening "${MINIO_CONSOLE_PORT}"; then
        echo "端口 ${MINIO_CONSOLE_PORT} 正在监听"
    else
        echo "端口 ${MINIO_CONSOLE_PORT} 未监听"
    fi

    local pid
    pid="$(discover_pid || true)"
    if [ -n "${pid}" ]; then
        echo "发现 minio 进程 PID: ${pid}"
    fi

    local log_path
    log_path="$(log_file)"
    if [ -f "${log_path}" ]; then
        echo "最近日志:"
        tail -n 20 "${log_path}" || true
    fi
}

show_help() {
    cat <<EOF
用法:
  ./check-minio.sh
  ./check-minio.sh start
  ./check-minio.sh stop
  ./check-minio.sh restart
  ./check-minio.sh status
  ./check-minio.sh logs
  ./check-minio.sh urls
  ./check-minio.sh check

默认行为:
  不带参数时等同于 start

可配置环境变量:
  MINIO_DIR=${MINIO_DIR}
  MINIO_CMD=${MINIO_CMD}
  MINIO_API_PORT=${MINIO_API_PORT}
  MINIO_CONSOLE_PORT=${MINIO_CONSOLE_PORT}
  MINIO_PATTERN=${MINIO_PATTERN}
EOF
}

main() {
    local command="${1:-start}"
    case "${command}" in
        start)
            start_minio
            ;;
        stop)
            stop_minio
            ;;
        restart)
            stop_minio
            echo ""
            start_minio
            ;;
        status)
            status_minio
            ;;
        logs)
            logs_minio
            ;;
        urls)
            show_urls
            ;;
        check)
            if health_check; then
                echo "minio 健康检查通过"
                exit 0
            fi
            print_diagnostics
            exit 1
            ;;
        -h|--help|help)
            show_help
            ;;
        *)
            echo "不支持的命令: ${command}"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

main "$@"

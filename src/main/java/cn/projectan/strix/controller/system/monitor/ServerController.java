package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.other.monitor.server.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * @author 安炯奕
 * @date 2022/9/30 21:57
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/server")
public class ServerController extends BaseSystemController {

    @GetMapping()
    @PreAuthorize("@ss.hasRead('System_Monitor_Server')")
    public RetResult<Object> getServerInfo() {
        Server server = new Server();
        server.loadAll();
        return RetMarker.makeSuccessRsp(Collections.singletonMap("server", server));
    }

}

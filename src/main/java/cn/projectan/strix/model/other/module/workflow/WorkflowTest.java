//package cn.projectan.strix.model.other.module.workflow;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.flow.model.Node;
//
//import java.util.List;
//import java.util.Scanner;
//import java.util.stream.Collectors;
//
/// **
// * 工作流
// *
// * @author ProjectAn
// * @date 2024/9/24 05:07
// */
//public class WorkflowTest {
//
//    private static final ObjectMapper objectMapper = new ObjectMapper();
//
//    private static String currentId = "root";
//
//    public static void main(String[] args) throws JsonProcessingException {
//        List<Node> nodes = objectMapper.readValue(json, new TypeReference<>() {
//        });
//
//        while (true) {
//            Node node = findNodeById(nodes, currentId);
//            if (node == null) {
//                System.out.println("未找到节点：" + currentId);
//                break;
//            }
//
//            handleNode(node);
//            waitNextNode(nodes, currentId);
//        }
//    }
//
//    public static Node findNodeById(List<Node> nodes, String id) {
//        return nodes.stream()
//                .filter(node -> node.getId().equals(id))
//                .findFirst()
//                .orElse(null);
//    }
//
//    public static void handleNode(Node node) {
//        if ("conditions".equals(node.getType())) {
//            handleConditions(node);
//        } else {
//            System.out.println("当前节点：" + node.getId() + "，" + node.getName());
//        }
//    }
//
//    public static void handleConditions(Node node) {
//        System.out.println("当前处于总分支：" + node.getId() + "，" + node.getName());
//        node.getBranches().forEach(condition ->
//                System.out.println("条件分支：" + condition.getId() + "，" + condition.getName())
//        );
//        waitChooseBranch(node);
//    }
//
//    public static void waitNextNode(List<Node> nodes, final String id) {
//        // 任意键继续
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("任意键继续...");
//        scanner.next();
//        // 找到下一个节点
//        List<Node> list = nodes.stream()
//                .filter(node -> id.equals(node.getParentId()))
//                .collect(Collectors.toList());
//        if (list.size() == 1) {
//            currentId = list.getFirst().getId();
//        } else {
//            Node node = findNodeById(nodes, id);
//            if (node != null && node.getConditionsId() != null) {
//                System.out.println("条件分支结束...");
//                waitNextNode(nodes, node.getConditionsId());
//            } else {
//                System.out.println("未找到节点: " + id + "的下一个节点.");
//                currentId = null;
//            }
//        }
//    }
//
//    public static void waitChooseBranch(Node node) {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("请选择分支：");
//        String input = scanner.nextLine();
//
//        node.getBranches().stream()
//                .filter(branch -> branch.getId().equals(input))
//                .findFirst()
//                .ifPresentOrElse(
//                        branch -> currentId = branch.getId(),
//                        () -> System.out.println("未找到分支：" + input)
//                );
//    }
//
//    public static boolean waitUserInput() {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("是否审批通过(Y/N)：");
//        String input = scanner.nextLine();
//        return "Y".equalsIgnoreCase(input);
//    }
//
//
//    private final static String json = "[{\"id\":\"root\",\"name\":\"root节点\",\"desc\":\"root节点...\",\"type\":\"root\",\"props\":null,\"parentId\":null,\"parentType\":null,\"branches\":null},{\"id\":\"zrulgt0xlhq\",\"name\":\"新审批人\",\"desc\":\"新审批人...\",\"type\":\"approval\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"timeLimit\":{\"value\":48,\"unit\":\"HOUR\",\"handler\":\"NOTIFY\"},\"reject\":{\"type\":\"END\",\"nodeId\":\"\"}},\"parentId\":\"root\",\"parentType\":\"root\",\"conditionsId\":null,\"branches\":[]},{\"id\":\"lgskk5z089q\",\"name\":\"新办理人\",\"desc\":\"新办理人...\",\"type\":\"task\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"timeLimit\":{\"value\":24,\"unit\":\"HOUR\",\"handler\":\"NOTIFY\"}},\"parentId\":\"zrulgt0xlhq\",\"parentType\":\"approval\",\"conditionsId\":null,\"branches\":[]},{\"id\":\"j1vjzcvfqk\",\"name\":\"新conditions\",\"desc\":\"新conditions...\",\"type\":\"conditions\",\"props\":{},\"parentId\":\"lgskk5z089q\",\"parentType\":\"task\",\"conditionsId\":null,\"branches\":[{\"id\":\"vvzwydys6kg\",\"name\":\"新条件0\",\"desc\":\"新条件...\",\"type\":\"condition\",\"props\":{\"type\":\"AND\",\"groups\":[{\"type\":\"AND\",\"conditions\":[]}]},\"parentId\":\"j1vjzcvfqk\",\"parentType\":\"conditions\",\"conditionsId\":\"j1vjzcvfqk\",\"branches\":[]},{\"id\":\"ouq2t6d8ngs\",\"name\":\"新条件1\",\"desc\":\"新条件...\",\"type\":\"condition\",\"props\":{\"type\":\"AND\",\"groups\":[{\"type\":\"AND\",\"conditions\":[]}]},\"parentId\":\"j1vjzcvfqk\",\"parentType\":\"conditions\",\"conditionsId\":\"j1vjzcvfqk\",\"branches\":[]}]},{\"id\":\"6840oswmaxi\",\"name\":\"新审批人\",\"desc\":\"新审批人...\",\"type\":\"approval\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"timeLimit\":{\"value\":48,\"unit\":\"HOUR\",\"handler\":\"NOTIFY\"},\"reject\":{\"type\":\"END\",\"nodeId\":\"\"}},\"parentId\":\"vvzwydys6kg\",\"parentType\":\"condition\",\"conditionsId\":\"j1vjzcvfqk\",\"branches\":[]},{\"id\":\"2lbjbjglm2k\",\"name\":\"新办理人\",\"desc\":\"新办理人...\",\"type\":\"task\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"timeLimit\":{\"value\":24,\"unit\":\"HOUR\",\"handler\":\"NOTIFY\"}},\"parentId\":\"ouq2t6d8ngs\",\"parentType\":\"condition\",\"conditionsId\":\"j1vjzcvfqk\",\"branches\":[]},{\"id\":\"cb4c05fhhvq\",\"name\":\"新conditions\",\"desc\":\"新conditions...\",\"type\":\"conditions\",\"props\":{},\"parentId\":\"2lbjbjglm2k\",\"parentType\":\"task\",\"conditionsId\":\"j1vjzcvfqk\",\"branches\":[{\"id\":\"jhpzw0gnb8a\",\"name\":\"新条件0\",\"desc\":\"新条件...\",\"type\":\"condition\",\"props\":{\"type\":\"AND\",\"groups\":[{\"type\":\"AND\",\"conditions\":[]}]},\"parentId\":\"cb4c05fhhvq\",\"parentType\":\"conditions\",\"conditionsId\":\"cb4c05fhhvq\",\"branches\":[]},{\"id\":\"x6dhw7htub\",\"name\":\"新条件1\",\"desc\":\"新条件...\",\"type\":\"condition\",\"props\":{\"type\":\"AND\",\"groups\":[{\"type\":\"AND\",\"conditions\":[]}]},\"parentId\":\"cb4c05fhhvq\",\"parentType\":\"conditions\",\"conditionsId\":\"cb4c05fhhvq\",\"branches\":[]}]},{\"id\":\"wahxrdlo7ve\",\"name\":\"新审批人\",\"desc\":\"新审批人...\",\"type\":\"approval\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"timeLimit\":{\"value\":48,\"unit\":\"HOUR\",\"handler\":\"NOTIFY\"},\"reject\":{\"type\":\"END\",\"nodeId\":\"\"}},\"parentId\":\"jhpzw0gnb8a\",\"parentType\":\"condition\",\"conditionsId\":\"cb4c05fhhvq\",\"branches\":[]},{\"id\":\"8n71x9evio9\",\"name\":\"新抄送人\",\"desc\":\"新抄送人...\",\"type\":\"cc\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"allowAdd\":false},\"parentId\":\"x6dhw7htub\",\"parentType\":\"condition\",\"conditionsId\":\"cb4c05fhhvq\",\"branches\":[]},{\"id\":\"j2c2ws7gqe\",\"name\":\"新抄送人\",\"desc\":\"新抄送人...\",\"type\":\"cc\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"allowAdd\":false},\"parentId\":\"cb4c05fhhvq\",\"parentType\":\"conditions\",\"conditionsId\":\"j1vjzcvfqk\",\"branches\":[]},{\"id\":\"wgjx6gwbl5c\",\"name\":\"新办理人\",\"desc\":\"新办理人...\",\"type\":\"task\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"timeLimit\":{\"value\":24,\"unit\":\"HOUR\",\"handler\":\"NOTIFY\"}},\"parentId\":\"j1vjzcvfqk\",\"parentType\":\"conditions\",\"conditionsId\":null,\"branches\":[]},{\"id\":\"76xir1rjbow\",\"name\":\"新抄送人\",\"desc\":\"新抄送人...\",\"type\":\"cc\",\"props\":{\"assign\":{\"type\":\"USER\",\"id\":[],\"mode\":\"ALL\"},\"allowAdd\":false},\"parentId\":\"wgjx6gwbl5c\",\"parentType\":\"task\",\"conditionsId\":null,\"branches\":[]}]";
//
//}

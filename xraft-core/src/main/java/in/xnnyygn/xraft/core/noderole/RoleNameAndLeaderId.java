package in.xnnyygn.xraft.core.noderole;

import in.xnnyygn.xraft.core.node.NodeId;

public class RoleNameAndLeaderId {

    private final RoleName roleName;
    private NodeId leaderId;

    public RoleNameAndLeaderId(RoleName roleName, NodeId leaderId) {
        this.roleName = roleName;
        this.leaderId = leaderId;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

}

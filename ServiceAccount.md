在 Kubernetes 上搭建 Flink HA 高可用集群的主要目标是实现 Flink 集群的高可用性，使得 JobManager 节点出现故障时可以自动切换到备用节点，保证任务的持续运行。以下是一个基于 Kubernetes 搭建 Flink 高可用集群的详细步骤。

### 高可用架构概述

在 Kubernetes 中的 Flink 高可用架构如下：
- **多个 JobManager**：至少两个 JobManager，一个作为主节点，其他作为备用节点。
- **多个 TaskManager**：根据实际工作负载和任务需求进行配置和水平扩展。
- **Kubernetes ConfigMap**：Flink 使用 ConfigMap 作为持久化存储，用来存储 Leader 选举的元数据，而不是使用传统的 Zookeeper。
- **Kubernetes ServiceAccount**：用于控制 Flink 访问 Kubernetes API 的权限，进行 ConfigMap 的读取和写入。

### 前提条件

- Kubernetes 集群（例如 Minikube、EKS、GKE、AKS 等）。
- Kubernetes 命令行工具（kubectl）。
- Docker 镜像仓库，用于存储 Flink 镜像。

### 步骤 1：准备 Flink 镜像

可以直接使用官方 Flink Docker 镜像，如：`apache/flink:1.17.0`。如果有特殊需求，可以根据需要自定义 Docker 镜像。

### 步骤 2：配置 Kubernetes YAML 文件

#### 2.1 **配置 Flink 高可用参数**

在 Flink 配置文件 `flink-conf.yaml` 中，添加以下内容：

```yaml
# Flink 高可用配置
high-availability: org.apache.flink.kubernetes.highavailability.KubernetesHaServicesFactory
high-availability.storageDir: s3://flink/ha/  # 持久化存储路径（如 S3、NFS、HDFS）
high-availability.cluster-id: flink-cluster  # 唯一标识集群的 ID
high-availability.kubernetes.namespace: default  # Kubernetes 中的命名空间
high-availability.kubernetes.leader-election.config-map.name: flink-leader-election

# JobManager 配置
jobmanager.rpc.address: flink-jobmanager  # JobManager 服务名
jobmanager.rpc.port: 6123
jobmanager.heap.size: 1024m

# TaskManager 配置
taskmanager.numberOfTaskSlots: 3
taskmanager.memory.process.size: 2048m
```

### 步骤 3：创建 Kubernetes ConfigMap 和 ServiceAccount

#### 3.1 **创建 Flink 配置的 ConfigMap**

```bash
kubectl create configmap flink-config --from-file=flink-conf.yaml
```

#### 3.2 **创建 Flink 访问 Kubernetes API 的 ServiceAccount**

定义一个 `ServiceAccount`：

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: flink-service-account
  namespace: default
```

#### 3.3 **创建 Role 和 RoleBinding**

为了让 Flink 能够访问 ConfigMap，需要创建一个 `Role` 和 `RoleBinding`。

1. 创建 Role：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: flink-role
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

2. 创建 RoleBinding：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: flink-role-binding
  namespace: default
subjects:
- kind: ServiceAccount
  name: flink-service-account
  namespace: default
roleRef:
  kind: Role
  name: flink-role
  apiGroup: rbac.authorization.k8s.io
```

### 步骤 4：部署 JobManager 和 TaskManager

#### 4.1 **JobManager 部署 YAML**

创建一个名为 `jobmanager-deployment.yaml` 的文件：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: flink-jobmanager
  labels:
    app: flink
    role: jobmanager
spec:
  replicas: 2  # 两个 JobManager 实例，一主一备
  selector:
    matchLabels:
      app: flink
      role: jobmanager
  template:
    metadata:
      labels:
        app: flink
        role: jobmanager
    spec:
      serviceAccountName: flink-service-account  # 绑定 ServiceAccount
      containers:
      - name: jobmanager
        image: apache/flink:1.17.0
        args: ["jobmanager"]
        ports:
        - containerPort: 6123  # RPC 端口
        - containerPort: 8081  # Web UI 端口
        volumeMounts:
        - name: flink-config-volume
          mountPath: /opt/flink/conf
      volumes:
      - name: flink-config-volume
        configMap:
          name: flink-config
```

#### 4.2 **JobManager Service YAML**

创建一个名为 `jobmanager-service.yaml` 的文件：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: flink-jobmanager
spec:
  ports:
  - port: 6123
    name: rpc
  - port: 8081
    name: webui
  selector:
    app: flink
    role: jobmanager
```

#### 4.3 **TaskManager 部署 YAML**

创建一个名为 `taskmanager-deployment.yaml` 的文件：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: flink-taskmanager
  labels:
    app: flink
    role: taskmanager
spec:
  replicas: 3  # 根据需求设置 TaskManager 的数量
  selector:
    matchLabels:
      app: flink
      role: taskmanager
  template:
    metadata:
      labels:
        app: flink
        role: taskmanager
    spec:
      serviceAccountName: flink-service-account  # 绑定 ServiceAccount
      containers:
      - name: taskmanager
        image: apache/flink:1.17.0
        args: ["taskmanager"]
        ports:
        - containerPort: 6121  # 数据传输端口
        - containerPort: 6122  # 流式传输端口
        volumeMounts:
        - name: flink-config-volume
          mountPath: /opt/flink/conf
      volumes:
      - name: flink-config-volume
        configMap:
          name: flink-config
```

### 步骤 5：应用 Kubernetes 配置

将所有创建的 YAML 文件应用到 Kubernetes 中：

```bash
kubectl apply -f jobmanager-deployment.yaml
kubectl apply -f jobmanager-service.yaml
kubectl apply -f taskmanager-deployment.yaml
```

### 步骤 6：验证集群状态

1. **检查 Pod 状态**：

   ```bash
   kubectl get pods
   ```

2. **访问 Flink Web UI**：通过 JobManager 服务的 `8081` 端口来访问 Flink 的 Web UI，查看集群的状态。

3. **测试高可用**：尝试删除一个 JobManager 的 Pod，观察备用 JobManager 是否接管了主 JobManager 的角色。

### 总结

通过上述步骤，我们使用 Kubernetes 部署了一个高可用的 Flink 集群。该方案中：

- 使用 Kubernetes 的 ConfigMap 作为 Flink 的高可用存储，无需 Zookeeper。
- 通过 Kubernetes Deployment 和 ServiceAccount 来管理 Flink 的 JobManager 和 TaskManager 实例，保证集群的高可用性和安全性。

这种配置方案利用 Kubernetes 的原生功能（如 Deployment、ConfigMap、ServiceAccount 和 RBAC）来实现 Flink 的高可用性，减少了对外部服务（如 Zookeeper）的依赖，同时保持了稳定性和灵活性。
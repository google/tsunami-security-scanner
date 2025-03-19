
# Cleanup actions

In our example, none of the defined actions will modify the target. But what if
we had some actions that we want to clean-up after? Cleanup actions are the
solution.

Let us assume an arbitrary example that would cause a change on the target:

```proto
actions: {
  name: "create_docker_container"
  http_request: {
    method: POST
    uri: "/api/v1/create"
    data: "name=MySuperContainer"
    response: {
      http_status: 200
    }
  }
}
```

In that example, if the request is successful, a new container will be created
on the target, but we want to make sure to delete that container afterwards.
Because cleanup actions are normal actions, we can start by writing the deletion
request:

```proto
actions: {
  name: "cleanup_container"
  http_request: {
    method: POST
    uri: "/api/v1/delete"
    data: "name=MySuperContainer"
    response: {
      http_status: 200
    }
  }
}
```

This action will ensure the container is deleted. But now, how do we make sure
it is executed after and only if the container has been created? We register
it as a cleanup of the initial action:

```proto
actions: {
  name: "create_docker_container"
  http_request: {
    method: POST
    uri: "/api/v1/create"
    data: "name=MySuperContainer"
    response: {
      http_status: 200
    }
  }
  cleanup_actions: "cleanup_container"
}
```

Note the newly added `cleanup_actions` entry. This will ensure the following:

- If `create_docker_container` is not executed or fail, the cleanup action will
not be executed;
- If the `create_docker_container` is executed successfully, the cleanup action
will always be executed at the end of the current workflow run;

## What is next

[Writing unit tests](08-writing-unit-tests)

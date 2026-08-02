$ErrorActionPreference = 'Stop'
$base = 'http://localhost:9000/house-service'

function Login($user, $pwd) {
  $body = "{`"username`":`"$user`",`"password`":`"$pwd`"}"
  $res = Invoke-RestMethod -Uri "$base/user/login" -Method Post -ContentType 'application/json' -Body $body
  return @{ token = $res.data.token; id = $res.data.userInfo.id }
}

function JsonBody($obj) { return $obj | ConvertTo-Json -Depth 8 -Compress }

# 1. zhangsan login
$zs = Login 'zhangsan' '123456'
$zsHeaders = @{ Authorization = $zs.token }
Write-Output "[1] zhangsan login ok, id=$($zs.id)"

# 2. save-flow: Dispatch -> ReqDesign -> End
$saveObj = @{
  templateId = 2
  nodes = @(
    @{ node = @{ nodeName="Dispatch"; nodeKey="node_start"; sortNum=0; nodeType=1; assignNext=1; nodeTips="" }
       fields = @(
         @{ fieldKey="req_no"; fieldLabel="ReqNo"; fieldType="text"; required=1; placeholder="input req no" },
         @{ fieldKey="req_date"; fieldLabel="ReqDate"; fieldType="date"; required=1 }
       )
    },
    @{ node = @{ nodeName="ReqDesign"; nodeKey="node_req"; sortNum=1; nodeType=2; assignNext=1; nodeTips="" }
       fields = @(
         @{ fieldKey="design_desc"; fieldLabel="DesignDesc"; fieldType="textarea"; required=1 }
       )
    },
    @{ node = @{ nodeName="End"; nodeKey="node_end"; sortNum=2; nodeType=3; assignNext=0; nodeTips="" }
       fields = @()
    }
  )
}
$saveRes = Invoke-RestMethod -Uri "$base/flow-template/save-flow" -Method Put -ContentType 'application/json' -Body (JsonBody $saveObj) -Headers $zsHeaders
Write-Output "[2] save-flow: code=$($saveRes.code)"

# 3. dispatch task to zhangsan
$createObj = @{
  templateId = 2
  taskName = "ImproveTestTask"
  taskDesc = "e2e test for improvement"
  startTime = "2026-08-01 00:00:00"
  endTime = "2026-08-31 23:59:59"
  firstHandlerIds = @($zs.id)
}
$createRes = Invoke-RestMethod -Uri "$base/flow-task/create" -Method Post -ContentType 'application/json' -Body (JsonBody $createObj) -Headers $zsHeaders
Write-Output "[3] create task: code=$($createRes.code) msg=$($createRes.message)"

# find task id
$taskListRes = Invoke-RestMethod -Uri "$base/flow-task/list" -Method Post -ContentType 'application/json' -Body '{"page":1,"limit":5}' -Headers $zsHeaders
$task = ($taskListRes.data.records | Where-Object { $_.taskName -eq "ImproveTestTask" })[0]
$taskId = $task.id
Write-Output "[3] taskId=$taskId"

# 4. getDetail: verify templateNodes
$detail = Invoke-RestMethod -Uri "$base/flow-task/$taskId" -Method Get -Headers $zsHeaders
$tplNodes = $detail.data.templateNodes
Write-Output "[4] templateNodes count=$($tplNodes.Count)"
$tplNodes | ForEach-Object { Write-Output "   - nodeId=$($_.id) name=$($_.nodeName) type=$($_.nodeType)" }

# 5. submit step1 (pass) - zhangsan
$curFields = $detail.data.currentNodeFields
$curTaskNode = ($detail.data.taskNodes | Where-Object { $_.submitStatus -eq 0 })[0]
$formData = @()
foreach ($f in $curFields) {
  $formData += @{ fieldId=$f.id; fieldKey=$f.fieldKey; fieldValue="test_value_$($f.fieldKey)" }
}
$submitObj = @{
  taskId=$taskId; taskNodeId=$curTaskNode.taskNodeId
  formData=$formData; action="pass"; nextHandlerUserId=$zs.id
}
$subRes = Invoke-RestMethod -Uri "$base/flow-task/submit" -Method Post -ContentType 'application/json' -Body (JsonBody $submitObj) -Headers $zsHeaders
Write-Output "[5] submit step1 (pass): code=$($subRes.code) msg=$($subRes.message)"

# 6. getDetail: verify step1 formDataList + action
$detail2 = Invoke-RestMethod -Uri "$base/flow-task/$taskId" -Method Get -Headers $zsHeaders
$doneNode = ($detail2.data.taskNodes | Where-Object { $_.submitStatus -eq 1 })[0]
Write-Output "[6] step1 done node: name=$($doneNode.nodeName) action=$($doneNode.action) formDataList count=$($doneNode.formDataList.Count)"
$doneNode.formDataList | ForEach-Object { Write-Output "   - $($_.fieldLabel) = $($_.fieldValue)" }

# 7. submit step2 (reject) - zhangsan
$curFields2 = $detail2.data.currentNodeFields
$curTaskNode2 = ($detail2.data.taskNodes | Where-Object { $_.submitStatus -eq 0 })[0]
$formData2 = @()
foreach ($f in $curFields2) { $formData2 += @{ fieldId=$f.id; fieldKey=$f.fieldKey; fieldValue="reject_reason" } }
$rejectObj = @{ taskId=$taskId; taskNodeId=$curTaskNode2.taskNodeId; formData=$formData2; action="reject" }
$rejRes = Invoke-RestMethod -Uri "$base/flow-task/submit" -Method Post -ContentType 'application/json' -Body (JsonBody $rejectObj) -Headers $zsHeaders
Write-Output "[7] submit step2 (reject): code=$($rejRes.code) msg=$($rejRes.message)"

# 8. getDetail: verify task back to start node, step2 action=1
$detail3 = Invoke-RestMethod -Uri "$base/flow-task/$taskId" -Method Get -Headers $zsHeaders
$task3 = $detail3.data.task
$startTplNode = ($detail3.data.templateNodes | Where-Object { $_.nodeType -eq 1 })[0]
$rejectedNode = ($detail3.data.taskNodes | Where-Object { $_.action -eq 1 })[0]
Write-Output "[8] task currentNodeId=$($task3.currentNodeId) (start=$($startTplNode.id)) match=$($task3.currentNodeId -eq $startTplNode.id)"
Write-Output "[8] rejected node: name=$($rejectedNode.nodeName) action=$($rejectedNode.action)"
$pendingNodes = $detail3.data.taskNodes | Where-Object { $_.submitStatus -eq 0 }
Write-Output "[8] pending taskNodes count=$($pendingNodes.Count) (should be 1 = start node)"

# 9. re-submit step1 (pass) -> step2 (pass) -> end (pass) to complete
# step1 again
$curFields4 = $detail3.data.currentNodeFields
$curTaskNode4 = ($detail3.data.taskNodes | Where-Object { $_.submitStatus -eq 0 })[0]
$fd4 = @(); foreach ($f in $curFields4) { $fd4 += @{ fieldId=$f.id; fieldKey=$f.fieldKey; fieldValue="redo_$($f.fieldKey)" } }
$s4 = @{ taskId=$taskId; taskNodeId=$curTaskNode4.taskNodeId; formData=$fd4; action="pass"; nextHandlerUserId=$zs.id }
Invoke-RestMethod -Uri "$base/flow-task/submit" -Method Post -ContentType 'application/json' -Body (JsonBody $s4) -Headers $zsHeaders | Out-Null
Write-Output "[9] re-submit step1 (pass) ok"

# step2 pass
$d5 = Invoke-RestMethod -Uri "$base/flow-task/$taskId" -Method Get -Headers $zsHeaders
$cf5 = $d5.data.currentNodeFields; $tn5 = ($d5.data.taskNodes | Where-Object { $_.submitStatus -eq 0 })[0]
$fd5 = @(); foreach ($f in $cf5) { $fd5 += @{ fieldId=$f.id; fieldKey=$f.fieldKey; fieldValue="pass_$($f.fieldKey)" } }
$s5 = @{ taskId=$taskId; taskNodeId=$tn5.taskNodeId; formData=$fd5; action="pass"; nextHandlerUserId=$zs.id }
Invoke-RestMethod -Uri "$base/flow-task/submit" -Method Post -ContentType 'application/json' -Body (JsonBody $s5) -Headers $zsHeaders | Out-Null
Write-Output "[9] submit step2 (pass) ok"

# end pass
$d6 = Invoke-RestMethod -Uri "$base/flow-task/$taskId" -Method Get -Headers $zsHeaders
$tn6 = ($d6.data.taskNodes | Where-Object { $_.submitStatus -eq 0 })[0]
$s6 = @{ taskId=$taskId; taskNodeId=$tn6.taskNodeId; formData=@(); action="pass" }
Invoke-RestMethod -Uri "$base/flow-task/submit" -Method Post -ContentType 'application/json' -Body (JsonBody $s6) -Headers $zsHeaders | Out-Null
Write-Output "[9] submit end (pass) ok"

# 10. verify task completed
$detailF = Invoke-RestMethod -Uri "$base/flow-task/$taskId" -Method Get -Headers $zsHeaders
Write-Output "[10] final task status=$($detailF.data.task.status) (2=completed) finishedNodeCount=$($detailF.data.task.finishedNodeCount)"

# 11. data admin: stats
$statsRes = Invoke-RestMethod -Uri "$base/flow-data/stats" -Method Get -Headers $zsHeaders
Write-Output "[11] stats: total=$($statsRes.data.totalTasks) running=$($statsRes.data.runningTasks) finished=$($statsRes.data.finishedTasks) myTodo=$($statsRes.data.myTodoCount)"

# summary
$ok = ($tplNodes.Count -eq 3) -and ($doneNode.formDataList.Count -gt 0) -and ($task3.currentNodeId -eq $startTplNode.id) -and ($rejectedNode.action -eq 1) -and ($detailF.data.task.status -eq 2)
Write-Output ""
if ($ok) { Write-Output "========== ALL PASS ==========" } else { Write-Output "========== SOME CHECK FAILED ==========" }

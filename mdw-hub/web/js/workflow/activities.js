'use strict';

var activityMod = angular.module('activities', ['mdw']);

activityMod.controller('ActivitiesController', ['$scope', '$http', '$uibModal', 'mdw', 'util', 'ACTIVITY_STATUSES',
                                                function($scope, $http, $uibModal, mdw, util, ACTIVITY_STATUSES) {

  // two-way bound to/from directive
  $scope.model = {};

  $scope.model.activityFilter = sessionStorage.getItem('activityFilter');
  if ($scope.model.activityFilter)
    $scope.model.activityFilter = JSON.parse($scope.model.activityFilter);

  $scope.defaultFilter = {
    status: '[Stuck]',
    sort: 'startDate',
    descending: true
  };

  $scope.resetFilter = function() {
    $scope.model.activityFilter = Object.assign({}, $scope.defaultFilter);
    sessionStorage.removeItem('activityFilter');
    $scope.closePopover();
  };

  if (!$scope.model.activityFilter) {
    $scope.resetFilter();
  }
  else {
    if ($scope.model.activityFilter.startDate)
      $scope.model.activityFilter.startDate = util.serviceDate(new Date($scope.model.activityFilter.startDate));
  }

  $scope.isDefaultFilter = function() {
    for (let key of Object.keys($scope.model.activityFilter)) {
      if (!$scope.model.activityFilter[key] && !$scope.defaultFilter[key])
        continue;
      if (key !== 'sort' && key !== 'descending' && key !== 'instanceId' && key !== 'activity' && key !== 'masterRequestId' &&
            $scope.model.activityFilter[key] !== $scope.defaultFilter[key]) {
        return false;
      }
    }
    return true;
  };

  $scope.model.activityList = {};

  $scope.allStatuses = ['[Any]','[Stuck]'].concat(ACTIVITY_STATUSES);
  $scope.selectedActivities = [];

  $scope.getSelectedActivities = function() {
    return $scope.model.activityList.getSelectedItems();
  };

  $scope.getSelectedActionableActivities = function(selectedRawActivities) {
    var selectedActivities = [];
    if (selectedRawActivities) {
    selectedRawActivities.forEach(function(activity) {
      if ($scope.canAction(activity, $scope.action))
        selectedActivities.push(activity);
      });
    }
    return selectedActivities;
  };

  $scope.confirmAction = function(action) {
    $scope.action = action;
    $scope.closePopover(); // popover should be closed
    mdw.messages = "";  // Clear any previous messages
    var selectedRawActivities = $scope.getSelectedActivities();
    // Filter out selected activities that are not actionable
    $scope.selectedActivities = $scope.getSelectedActionableActivities(selectedRawActivities);
    if ($scope.selectedActivities && $scope.selectedActivities.length > 0) {
      var modalInstance = $uibModal.open({
            scope: $scope,
            templateUrl: 'workflow/activityActionConfirm.html',
            controller: 'ActivitiesController',
            size: 'sm'
          });
    }
    else {
      if (selectedRawActivities && selectedRawActivities.length > 0)
        mdw.messages = "None of the selected activities can be actioned based on their current status and selected action";
      else
        mdw.messages = 'Please select Activity(s) to perform action on.';
    }
  };

  $scope.performActionOnActivities = function(action) {
    var selectedRawActivities = $scope.$parent.getSelectedActivities();
    var selectedActivities = $scope.$parent.selectedActivities;
    $scope.closePopover(); // popover should be closed

    if (selectedActivities && selectedActivities.length > 0) {
      console.log('Performing action: ' + $scope.action + ' on ' + selectedActivities.length + ' selected activitie(s)');
      if (selectedActivities.length !== selectedRawActivities.length)
        mdw.messages = "Some of the selected activities could not be actioned based on their current status and selected action";

      var instanceIds = [];
      selectedActivities.forEach(function(activity) {
        instanceIds.push(activity.id);
      });

      var errorHandler = function(data, status) {
        console.log('http: ' + status);
        $scope.model.activityList.reload(function(activityList) {
          $scope.updateOnActionError(data.status.message, instanceIds, activityList);
        });
      };
      var successHandler = function(data, status, headers, config) {
        if (data.status.code >= 300) {
          $scope.$parent.model.activityList.reload(function(activityList) {
            $scope.updateOnActionError(data.status.message, instanceIds, activityList);
          });
        }
        else {
          $scope.$close();
          $scope.$parent.model.activityList.reload();
        }
      };
      var actionSubUrl = '';
      if (!angular.isUndefined($scope.model.completionCode))
      {
        actionSubUrl = $scope.action + "/" + $scope.model.completionCode;
      }
      else {
        actionSubUrl = $scope.action;
      }
      for (var i = 0; i < selectedActivities.length; i++) {
        var request = $http({
          method : "post",
          url : mdw.roots.services + '/Services/Activities/' + selectedActivities[i].id + '/' + actionSubUrl + '?app=mdw-admin',
          data : {}
        });

        request.error(errorHandler).success(successHandler);
      }
    }
    else {
      mdw.messages = 'Please select Activity(s) to perform action on.';
    }
  };

  $scope.updateOnActionError = function(message, prevSelInstIds, activityList) {
    mdw.messages = message;
    $scope.$close();
    // find problem instance id if available
    var instIdIdx = message.indexOf('instance: ');
    if (instIdIdx !== -1) {
      var erroredInstId = parseInt(message.substring(instIdIdx + 10));
      if (erroredInstId) {
        var after = false;
        var newSelInstIds = [];
        prevSelInstIds.forEach(function(prevSelInstId) {
          if (prevSelInstId == erroredInstId)
            after = true;
          if (after) {
            newSelInstIds.push(prevSelInstId);
          }
        });
        activityList.forEach(function(activity) {
          if (newSelInstIds.indexOf(activity.id) >= 0) {
            activity.selected = true;
          }
        });
      }
    }
    $scope.$parent.digest(); // to show mdw.messages
  };

  // preselected activity
  if ($scope.model.activityFilter.activity) {
    var activitySpec = sessionStorage.getItem('activitySpec');
    if (activitySpec) {
      $scope.model.typeaheadMatchSelection = activitySpec;
    }
    else {
      $scope.model.typeaheadMatchSelection = $scope.model.activityFilter.activity;
    }
  }
  else {
    sessionStorage.removeItem('activitySpec');
    if ($scope.model.activityFilter.instanceId) {
      $scope.model.typeaheadMatchSelection = $scope.model.activityFilter.instanceId;
    }
  }

  // activity instanceId, masterRequestId, activity name
  $scope.findTypeaheadMatches = function(typed) {
    return $http.get(mdw.roots.services + '/services/Activities/definitions' + '?app=mdw-admin&find=' + typed).then(function(response) {
      // match on activity name
      if (response.data.activityInstances.length > 0) {
        var defMatches = [];
        response.data.activityInstances.forEach(function(actDef) {
          defMatches.push({
            type: 'activity',
            value: actDef.processName + ' v' + actDef.processVersion + ' ' + actDef.name + ' (' + actDef.definitionId + ')',
            id: actDef.processId + '%3A' + actDef.definitionId
          });
        });
        return defMatches;
      }
    });
  };

  $scope.clearTypeaheadFilters = function() {
    // check if defined to avoid triggering evaluation
    if ($scope.model.activityFilter.instanceId)
      $scope.model.activityFilter.instanceId = null;
    if ($scope.model.activityFilter.masterRequestId)
      $scope.model.activityFilter.masterRequestId = null;
    if ($scope.model.activityFilter.activity)
      $scope.model.activityFilter.activity = null;
  };

  $scope.$on('page-retrieved', function(event) {
    sessionStorage.setItem('activityFilter', JSON.stringify($scope.model.activityFilter));
  });

  $scope.typeaheadChange = function() {
    if ($scope.model.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };

  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.model.typeaheadMatchSelection.id)
      $scope.model.activityFilter[$scope.model.typeaheadMatchSelection.type] = $scope.model.typeaheadMatchSelection.id;
    else
      $scope.model.activityFilter[$scope.model.typeaheadMatchSelection.type] = $scope.model.typeaheadMatchSelection.value;
  };

  $scope.clearTypeahead = function() {
      $scope.model.typeaheadMatchSelection = null;
      $scope.clearTypeaheadFilters();
  };

  $scope.cancelAction = function(){
    $scope.$close();
  };

  $scope.canAction = function(activity, action) {
     if (action == 'Retry' && activity.status != 'Failed' && activity.status != 'Completed' && activity.status != 'Cancelled')
       return false;
     else
       return true;
  };

  $scope.getSelectedActivitiesMessage = function() {
  var selectedActivities = $scope.$parent.selectedActivities;
    if (selectedActivities && selectedActivities.length > 0) {
      var base = 'Do you want to perform the selected action on ';
      if (selectedActivities.length == 1)
        return base + 'the selected activity?';
      else
        return base + selectedActivities.length + ' activities?';
    }
  };

  $scope.goChart = function() {
    window.location = 'dashboard/activities';
  };

  $scope.goInstance = function(item) {
    sessionStorage.setItem('mdw-activityInstance', item.id);
    window.location = '#/workflow/processes/' + item.processInstanceId;
  };
}]);

activityMod.controller('ActivityController', ['$scope', '$http', '$route', 'Process', '$uibModal', '$routeParams', 'mdw', 'Activity',
                                            function($scope, $http, $route, Process, $uibModal, $routeParams, mdw, Activity) {
  $scope.model = {};
  $scope.model.singleActivity = true;

  var activityInstanceId = $scope.activityInstanceId ? $scope.activityInstanceId : $routeParams.instanceId;

  $scope.activity = Activity.retrieve({instanceId: activityInstanceId}, function(activity) {
    $scope.activity = activity;
    $scope.activity.name = $scope.activity.name;
    $scope.item = $scope.activity; // for activityItem template
  });
  $scope.process = Process.retrieve({activityInstanceId: $routeParams.instanceId});

  $scope.getSelectedActivitiesMessage = function() {
    return 'Do you want to perform "' + $scope.action + '" on activity: ' + $scope.activity.id + '?';
  };

  $scope.performActionOnActivities = function(action) {
     $scope.closePopover(); // popover should be closed

     var errorHandler = function(data, status) {
       console.log('http: ' + status);
     };
     var successHandler = function(data, status, headers, config) {
       if (data.status.code >= 300) {
       }
       else {
         $scope.$close();
       }
     };
     var actionSubUrl = '';
     if (!angular.isUndefined($scope.model.completionCode))
     {
       actionSubUrl = action + "/" + $scope.model.completionCode;
     }
     else {
       actionSubUrl = action;
     }
     var request = $http({
       method : "post",
       url : mdw.roots.services + '/Services/Activities/' + $scope.activity.id + '/' + actionSubUrl + '?app=mdw-admin',
       data : {}
     });
     $scope.$close();
     request.error(errorHandler).success(successHandler);
     $route.reload();
   };

   $scope.cancelAction = function(){
     $scope.$close();
   };

   $scope.confirmAction = function(action) {
     $scope.action = action;
     $scope.closePopover(); // popover should be closed
     var modalInstance = $uibModal.open({
       scope: $scope,
       templateUrl: 'workflow/activityActionConfirm.html',
       controller: 'ActivityController',
       size: 'sm'
     });
   };

   $scope.canAction = function(action) {
     if (action == 'Retry' && $scope.activity.status != 'Failed' && $scope.activity.status != 'Completed' && $scope.activity.status != 'Cancelled')
       return false;
     else
       return true;
   };
}]);

activityMod.controller('ActivityLogController', ['$scope', '$http', '$routeParams', 'mdw', 'util', 'EXCEL_DOWNLOAD',
                                            function($scope, $http, $routeParams, mdw, util, EXCEL_DOWNLOAD) {
  $scope.activity = {
    id: $routeParams.instanceId
  };

  $scope.title = 'Activity ' + $scope.activity.id + ' Log';

  $scope.logTime = 'DB Time';
  $scope.setLogTime = function(logTime) {
    $scope.logTime = logTime;
    document.getElementById('log-time-popover').click();
    util.calcLogTimes($scope.logData, logTime);
  };

  var url = mdw.roots.services + '/services/Activities/' + $scope.activity.id + '/log?app=mdw-admin';
  $http.get(url)
    .then(function(response) {
      $scope.logData = response.data;
      util.calcLogTimes($scope.logData, $scope.logTime);
      $scope.activity.processInstanceId = response.data.processInstanceId;
    }
  );

  $scope.downloadExcel = function() {
    window.location = url + '&' + EXCEL_DOWNLOAD;
  };
}]);

activityMod.factory('Activity', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Activities/:instanceId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);

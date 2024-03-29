<%--

    Copyright © 2014 Instituto Superior Técnico

    This file is part of FenixEdu CMS.

    FenixEdu CMS is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu CMS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu CMS.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<div class="page-header">
    <h1>${site.name.content}</h1>
    
    <h2>
        <small>
            <a href="${pageContext.request.contextPath}/cms/pages/advanced/${site.slug}">
                <spring:message code="page.edit.title"/> - ${page.name.content}
            </a>
        </small>
    </h2>

    <div class="row">
        <div class="col-sm-12">
            <c:if test="${page.site.published && page.published}">
                <a href="${page.address}" target="_blank" class="btn btn-default">Link</a>
            </c:if>
        </div>
    </div>
</div>

<div ng-app="componentsApp" ng-controller="ComponentController">
    <form id="mainForm" class="form-horizontal" action="" method="post" role="form">

        <div class="${emptyName ? "form-group has-error" : "form-group"}">
            <label for="inputEmail3" class="col-sm-2 control-label"><spring:message
                    code="page.edit.label.slug"/></label>

            <div class="col-sm-5">
                <div class="input-group">

                    <span class="input-group-addon"><code>/${site.baseUrl}/</code></span>
                    <input type="text" name="slug" class="form-control" id="inputEmail3"
                           placeholder="<spring:message code="page.edit.label.slug" />" value='${page.slug}' \>
                </div>
            </div>

        </div>

        <div class="${emptyName ? "form-group has-error" : "form-group"}">
            <label for="inputEmail3" class="col-sm-2 control-label"><spring:message
                    code="page.edit.label.name"/></label>

            <div class="col-sm-10">
                <input bennu-localized-string type="text" name="name" id="inputEmail3"
                       placeholder="<spring:message code="page.edit.label.name" />" value='${page.name.json()}' \>
                <c:if test="${emptyName != null}"><p class="text-danger"><spring:message
                        code="page.edit.error.emptyName"/></p>
                </c:if>
            </div>
        </div>


        <div class="form-group">
            <label for="inputEmail3" class="col-sm-2 control-label"><spring:message
                    code="page.edit.label.template"/></label>

            <div class="col-sm-10">
                <select name="template" class="form-control" id="tempate">
                    <option value="null">-</option>
                    <c:forEach var="i" items="${site.theme.allTemplates}">
                        <option value="${i.type}" ${i == page.template ? 'selected' : ''}>${i.name}</option>
                    </c:forEach>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label for="inputEmail3" class="col-sm-2 control-label">Can View</label>

            <div class="col-sm-10">
                <input bennu-group allow="public,users,managers,custom" name="viewGroup" type="text"
                       value="${ page.canViewGroup.expression }"/>
            </div>
        </div>

        <div class="form-group">
            <label for="inputEmail3" class="col-sm-2 control-label"><spring:message
                    code="site.create.label.published"/></label>

            <div class="col-sm-2">
                <input name="published" type="checkbox" value="true" ${page.published ? "checked='checked'" : ""}>
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button type="submit" onclick="$('#mainForm').submit()" class="btn btn-default btn-primary">
                    <spring:message
                            code="action.save"/></button>
            </div>
        </div>
    </form>
    <hr>
    <h3><spring:message code="page.edit.label.pageComponents"/>:</h3>

    <p>

    <div class="btn-group">
        <button type="button" class="btn btn-default dropdown-toggle btn-sm" data-toggle="dropdown">
            <spring:message code="page.edit.label.addComponent"/>
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu">
            <li ng-repeat="component in components">
                <a href="#" ng-if="component.stateless" ng-click="installStateless(component)">{{component.name}}</a>
                <a href="#" ng-if="!component.stateless" ng-click="openModal(component)">{{component.name}}</a>
            </li>
        </ul>
    </div>
    </p>

    <c:choose>
        <c:when test="${page.componentsSet.size() == 0}">
            <p><spring:message code="page.edit.label.emtpySiteMenus"/></p>
        </c:when>

        <c:otherwise>
            <table class="table table-striped">
                <thead>
                <tr>
                    <th><spring:message code="page.edit.label.name"/></th>
                    <th><spring:message code="page.edit.label.creationDate"/></th>
                    <th><spring:message code="page.edit.label.operations"/></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="m" items="${page.componentsSet}">
                    <tr>
                        <td>
                            <h5>${m.name}</h5>

                            <div>
                                <small>${m.description}</code></small>
                            </div>
                        </td>
                        <td>${m.creationDate.toString('dd MMMM yyyy, HH:mm', locale)}
                            <small>- ${m.createdBy.name}</small>
                        </td>
                        <td>
                            <div class="btn-group" role="group">
                                <a href="#" class="btn btn-danger btn-sm"
                                   onclick="document.getElementById('deleteComponentForm${m.externalId}').submit();"><spring:message
                                        code="action.delete"/></a>


                                <c:if test="${m.getClass().simpleName.equals('StaticPost')}">
                                    <a href="${m.post.getEditUrl()}" class="btn btn-default btn-sm" role="button"/>
                                    <spring:message code="action.edit"></spring:message>
                                    </a>
                                </c:if>
                                <c:if test="${m.getClass().simpleName.equals('ListCategoryPosts')}">
                                    <a href="${m.category.getEditUrl()}" class="btn btn-default btn-sm" role="button"/>
                                    <spring:message code="action.edit"></spring:message>
                                    </a>
                                </c:if>
                                <c:if test="${m.getClass().simpleName.equals('StrategyBasedComponent')}">
                                    <c:if test="${m.componentType().simpleName.equals('ListPosts')}">
                                        <a href="${pageContext.request.contextPath}/cms/posts/${page.site.slug}"
                                           role="button"
                                           class="btn btn-default btn-sm"/>
                                        <spring:message code="action.edit"></spring:message>
                                        </a>
                                    </c:if>
                                    <c:if test="${m.componentType().simpleName.equals('ListOfCategories')}">
                                        <a href="${pageContext.request.contextPath}/cms/categories/${page.site.slug}"
                                           role="button"
                                           class="btn btn-default btn-sm"/>
                                        <spring:message code="action.edit"></spring:message>
                                        </a>
                                    </c:if>
                                </c:if>
                                <form id="deleteComponentForm${m.externalId}"
                                      action="${pageContext.request.contextPath}/cms/components/${site.slug}/${page.slug}/deleteComponent/${m.getExternalId()}"
                                      method="POST"></form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>

    <div class="modal fade" id="componentModal" tabindex="-1" role="dialog" aria-labelledby="componentModal"
         aria-hidden="true">
        <form class="modal-dialog form-horizontal" ng-submit="createComponent()">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title">{{component.name}}</h4>
                </div>
                <div class="modal-body">
                    <div class="form-group" ng-repeat="item in data">
                        <label for="{{item.key}}" class="col-sm-2 control-label">{{item.title}} <span style="color: red"
                                                                                                      ng-if="item.required">*</span></label>

                        <div class="col-sm-10" ng-if="isSelect(item)">
                            <select class="form-control" ng-model="selected[item.key]" ng-required="item.required"
                                    ng-options="value.value as value.label for value in item.values">
                                <option value="">-- Pick One --</option>
                            </select>
                        </div>
                        <div class="col-sm-10" ng-if="!isSelect(item) && item.type != 'BOOLEAN'">
                            <input type="{{item.type}}" ng-model="selected[item.key]" class="form-control"
                                   ng-required="item.required"/>
                        </div>
                        <div class="col-sm-10" ng-if="item.type == 'BOOLEAN'">
                            <input type="checkbox" ng-model="selected[item.key]"/>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal"><spring:message
                            code="action.close"/></button>
                    <button type="submit" class="btn btn-primary"><spring:message code="action.create"/></button>
                </div>
            </div>
        </form>
    </div>
</div>


<link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/font-awesome.css"/>
${portal.toolkit()}
<script src="${pageContext.request.contextPath}/bennu-core/js/angular.min.js"></script>

<script type="text/javascript">
    angular.module('componentsApp', []).controller('ComponentController', ['$scope', '$http', function ($scope, $http) {
        $scope.components = ${availableComponents};
        $scope.installStateless = function (component) {
            $http.post('${pageContext.request.contextPath}/cms/components/${site.slug}/${page.slug}/createComponent', {type: component.type}).
                    success(function () {
                        location.reload();
                    });
        };
        $scope.openModal = function (component) {
            $scope.component = component;
            $("#componentModal").modal('show');
            $scope.selected = {};
            $http.post('${pageContext.request.contextPath}/cms/components/componentArguments/${page.externalId}?type=' + component.type).
                    success(function (data) {
                        $scope.data = data;
                    });
        };
        $scope.createComponent = function () {
            $http.post('${pageContext.request.contextPath}/cms/components/${site.slug}/${page.slug}/createComponent',
                    {type: $scope.component.type, parameters: $scope.selected}).success(function () {
                        location.reload();
                    });
        }
        $scope.isSelect = function (item) {
            return item.type == 'DOMAIN_OBJECT' || item.type == 'ENUM' || item.values.length > 0;
        }
    }]);
</script>
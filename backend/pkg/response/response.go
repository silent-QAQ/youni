package response

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type Response struct {
	Code int         `json:"code"`
	Msg  string      `json:"msg"`
	Data interface{} `json:"data,omitempty"`
}

func OK(c *gin.Context, data interface{}) {
	c.JSON(http.StatusOK, Response{
		Code: 200,
		Msg:  "ok",
		Data: data,
	})
}

func Error(c *gin.Context, httpCode int, code int, msg string) {
	c.JSON(httpCode, Response{
		Code: code,
		Msg:  msg,
	})
}

func BadRequest(c *gin.Context, msg string) {
	Error(c, http.StatusBadRequest, 400, msg)
}

func Unauthorized(c *gin.Context, msg string) {
	Error(c, http.StatusUnauthorized, 401, msg)
}

func Forbidden(c *gin.Context, msg string) {
	Error(c, http.StatusForbidden, 403, msg)
}

func NotFound(c *gin.Context, msg string) {
	Error(c, http.StatusNotFound, 404, msg)
}

func ServerError(c *gin.Context, msg string) {
	Error(c, http.StatusInternalServerError, 500, msg)
}

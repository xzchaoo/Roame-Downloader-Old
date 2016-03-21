# Linux cd命令行小工具 #
## 简介 ##
[路游网](http://www.roame.net/)专辑批量下载, 年久失修, 不知道还可用不.

路游网(ROAME)是一个提供图片的网站, 网站没有验证码机制... 连验证码都没有, 分分钟被搞定的节奏啊!
每个专辑下有许多图片, 每个专辑有分页, 需要不断遍历.
普通账号下载有限制, 20KB/S, 但是可以开多个账号, 同时进行下载, 意思就是说对于单个IP似乎没有限制, 为什么会这样?
图片的URL有时效性, 要在一定的时间内下载完, 否则就失效了.

采用消费者生产者模式
1. 若干线程不断下载
2. 一个线程不断产生下载任务(主要是要获得图片的url)
	1. 产生速度不要太快, 否则内存占用太多, URL可能会无效 

最后是被网站管理员发现了, 把我IP封了......


## 基本策略 ##
1. 由于网站没有验证码机制, 因此就是非常简单的模拟HTTP请求就行了.
2. 一个普通账号一次只能下载一张图片, 速度20KB/S左右.
3. 但是一个IP似乎可以开多个不同的账号进行下载, 这样速度就提高了.
4. 注意最后产生的图片的URL有实效性.

# LICENSE #
```
Copyright [2015] [xzchaoo(70862045@qq.com)]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
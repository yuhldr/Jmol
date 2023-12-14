# 说明

代码来自 [Jmol](https://sourceforge.net/projects/jmol/)，以后会尽可能根据他们的新发布进行补充这里的代码，我想要一个干干净净的 `jar` 程序源码


> 真心不会用邮件列表给 [Jmol](https://sourceforge.net/projects/jmol/) 提交代码

## 修改说明

- 支持 `jdk17-openjdk`

    不想 java8，现在springboot3都强推17了，编译中的部分警告已经清理，有强迫症

    ！尤其注意 `src/jspecview/api/JSVTreeNode.java` 里面的 `children()` 被我改成了 `children2()`（当然，相关的引用也改了）暂时没发现问题，不修改这个我是真编译不下去了。

- `ant` 秒编译

    不会也不想用 eclipe 这种远古IDE，目前可在 `archlinux` 编译通过，终端输入 `ant` 即可编译完成，前提安装了 `sudo pacman -S ant` 注意java环境选择 `jdk17-openjdk`

    此时，编译的文件在 `build/Jmol.jar` 以及 `/tmp/Jmol.jar`

- 不会用 `sourceforge`，所以放在这里

- 补充部分中文翻译

    原因同上，邮件列表不会用，也懒得学


- 只保留 `Java` 相关内容

    一个`jar` 这么强大，我很佩服，不想看到其他的。

    除了 `Java` 的代码和必要编译文件（我认为），其他 `js` 等内容我都删除了，实在受不了这么多乱七八糟的东西，我把代码理清楚差点没累死。
    

## 使用说明

看前面的 `ant` 秒编译（直接下载 `jar` 也行）

```bash
ant
```

运行 `build/Jmol.jar` 即可

```bash
java -jar build/Jmol.jar
```

在高分辨率下，界面是真小啊，应该不只我这么笨吧

```bash
java -Dsun.java2d.uiScale=2.5 -jar build/Jmol.jar
```

其中 `2.5` 是缩放率




突然发现，额，我干了啥，自己确实能编译了，但是这么多代码，我也修改不动啊。。。。

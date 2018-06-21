SlidingTabLayout for ViewPager 
==================================

[![Build Status](https://travis-ci.org/felipehjcosta/SlidingTabLayout.svg?branch=master)](https://travis-ci.org/felipehjcosta/SlidingTabLayout)

SlidingTabLayout is a rudimentary library that allows you to create a tab of viewpager with colored indicators:

  ![demo](screenShots/SlidingTabLayout.gif)

Usage
-----

```xml
<com.github.felipehjcosta.slidingtablayout.SlidingTabLayout
    android:id="@+id/slidingTabLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
val colors = intArrayOf(
        Color.parseColor("#7ED321"),
        Color.parseColor("#E5615C"),
        Color.parseColor("#40AAB9")
)
sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

container.adapter = sectionsPagerAdapter
slidingTabLayout.setDistributeEvenly(false)
slidingTabLayout.setCustomTabView(R.layout.indicator_view, R.id.indicator_title)
slidingTabLayout.setTabColorizer(SlidingTabLayout.SimpleTabColorizer(colors))
slidingTabLayout.setViewPager(container)
```

Download
--------

Gradle:
```groovy
compile 'com.github.felipehjcosta:slidingtablayout:1.0.0'
```
or Maven:
```xml
<dependency>
  <groupId>com.github.felipehjcosta</groupId>
  <artifactId>slidingtablayout</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

License
-------

MIT License

Copyright (c) 2017 Felipe Costa

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

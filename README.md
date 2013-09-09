Vega node for KNIME
===================

Batch conversion using [Vega](https://github.com/trifacta/vega) and Node.js for KNIME

Installation
------------

Currently only tested on Windows.

How to setup Windows and KNIME for vega nodejs:
 - install the _32_ bit nodejs (it also installs npm, adds to the PATH)
 - install Python 2.7, add to the PATH (most probably C:\Python27), also
add PYTHONPATH (this might be optional?) I used Python 2.7.5.
 - extract GTK 32 bit bundle to C:\GTK add C:\GTK\bin to the PATH.
 - preferably to a path with only ASCII, visible, non-space characters
npm install vega (in my example I used c:\tmp)
( - the embedded node packages might have to copied to the siblings of
vega, although this might be optional, not familiar with npm.)
 - the path of KNIME should contain python, and the c:\GTK\bin folders.
 - the KNIME instance should set the executable and the vega install
paths in the preferences.
 - the SVG extension is required to create SVG images to KNIME.

The following page might be more up-to-date: [Installing node canvas for Windows](https://github.com/benjamind/delarre.docpad/blob/master/src/documents/posts/installing-node-canvas-for-windows.html.md)


## License
Released under the [AGPL v3](http://www.gnu.org/licenses/agpl-3.0.html) license.

AM_CFLAGS = $(DEBUG_FLAGS)

lib_LTLIBRARIES = liblept.la
liblept_la_LIBADD = $(LIBM) $(ZLIB_LIBS) $(LIBPNG_LIBS) $(JPEG_LIBS) $(GIFLIB_LIBS) $(LIBTIFF_LIBS) $(LIBWEBP_LIBS) $(GDI_LIBS)

liblept_la_LDFLAGS = -no-undefined -version-info 4:1:0

liblept_la_SOURCES = adaptmap.c affine.c                        \
 affinecompose.c arrayaccess.c                                  \
 bardecode.c baseline.c bbuffer.c                               \
 bilateral.c bilinear.c binarize.c                              \
 binexpand.c binreduce.c                                        \
 blend.c bmf.c bmpio.c bmpiostub.c                              \
 boxbasic.c boxfunc1.c boxfunc2.c boxfunc3.c boxfunc4.c         \
 bytearray.c ccbord.c ccthin.c classapp.c                       \
 colorcontent.c coloring.c                                      \
 colormap.c colormorph.c	                                \
 colorquant1.c colorquant2.c                                    \
 colorseg.c colorspace.c                                        \
 compare.c conncomp.c convertfiles.c                            \
 convolve.c convolvelow.c correlscore.c                         \
 dewarp1.c dewarp2.c dewarp3.c dewarp4.c                        \
 dnabasic.c dwacomb.2.c dwacomblow.2.c                          \
 edge.c enhance.c                                               \
 fhmtauto.c fhmtgen.1.c fhmtgenlow.1.c			        \
 finditalic.c flipdetect.c fliphmtgen.c                         \
 fmorphauto.c fmorphgen.1.c fmorphgenlow.1.c                    \
 fpix1.c fpix2.c gifio.c gifiostub.c                            \
 gplot.c graphics.c graymorph.c graymorphlow.c                  \
 grayquant.c grayquantlow.c heap.c jbclass.c                    \
 jp2kio.c jp2kiostub.c jpegio.c jpegiostub.c                    \
 kernel.c leptwin.c libversions.c list.c maze.c                 \
 morph.c morphapp.c morphdwa.c morphseq.c                       \
 numabasic.c numafunc1.c numafunc2.c                            \
 pageseg.c paintcmap.c                                          \
 parseprotos.c partition.c                                      \
 pdfio.c pdfiostub.c                                            \
 pix1.c pix2.c pix3.c pix4.c pix5.c                             \
 pixabasic.c pixacc.c pixafunc1.c pixafunc2.c                   \
 pixalloc.c pixarith.c pixcomp.c pixconv.c	                \
 pixlabel.c pixtiling.c pngio.c pngiostub.c                     \
 pnmio.c pnmiostub.c projective.c	                        \
 psio1.c psio1stub.c psio2.c psio2stub.c                        \
 ptabasic.c ptafunc1.c ptra.c	                                \
 quadtree.c queue.c rank.c                                      \
 readbarcode.c readfile.c                                       \
 recogbasic.c recogdid.c recogident.c                           \
 recogtrain.c regutils.c                                        \
 rop.c ropiplow.c roplow.c                                      \
 rotate.c rotateam.c rotateamlow.c                              \
 rotateorth.c rotateorthlow.c rotateshear.c                     \
 runlength.c sarray.c                                           \
 scale.c scalelow.c	                                        \
 seedfill.c seedfilllow.c                                       \
 sel1.c sel2.c selgen.c                                         \
 shear.c skew.c	spixio.c                                        \
 stack.c sudoku.c textops.c                                     \
 tiffio.c tiffiostub.c 		                                \
 utils.c viewfiles.c                                            \
 warper.c watershed.c                                           \
 webpio.c webpiostub.c		                                \
 writefile.c zlibmem.c zlibmemstub.c

pkginclude_HEADERS = allheaders.h alltypes.h                    \
 array.h arrayaccess.h bbuffer.h bilateral.h bmf.h bmp.h        \
 ccbord.h dewarp.h endianness.h environ.h		        \
 gplot.h heap.h imageio.h jbclass.h                             \
 leptwin.h list.h	                                        \
 morph.h pix.h ptra.h queue.h readbarcode.h                     \
 recog.h regutils.h stack.h sudoku.h watershed.h

noinst_PROGRAMS = xtractprotos
LDADD = liblept.la

EXTRA_DIST = arrayaccess.h.vc                                   \
 endiantest.c endianness.h.dist	                                \
 hmttemplate1.txt hmttemplate2.txt                              \
 leptonica-license.txt		                                \
 morphtemplate1.txt morphtemplate2.txt

allheaders: $(liblept_la_SOURCES)
	@test -x xtractprotos || echo "First run 'make xtractprotos'"
	./xtractprotos -prestring=LEPT_DLL -protos=inline $(liblept_la_SOURCES)

dist-hook: 
	cp "$(distdir)"/endianness.h{.dist,}

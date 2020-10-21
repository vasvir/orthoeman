PDH=pandoc -s --toc --number-sections -V documentclass=report -c pandoc.css --metadata date="`date -u '+%Y-%m-%d'`"
PDP=pandoc -H disable_float.tex -s --toc --number-sections -V documentclass=report -c pandoc.css --metadata date="`date -u '+%Y-%m-%d'`"
TARGET:=$(shell ls *.md | head -n 1 | sed 's/\.md//g')
TARGETS=$(TARGET).html $(TARGET).pdf index.html

all: $(TARGETS)

clean:
	$(RM) $(TARGETS)

%.html: %.md
	$(PDH) $< -o $@

%.pdf: %.md
	$(PDP) $< -o $@

index.html: $(TARGET).html
	ln -s $< $@

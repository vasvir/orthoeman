PD=pandoc -H disable_float.tex -s --toc --number-sections -V documentclass=report -c pandoc.css --metadata date="`date -u '+%Y-%m-%d'`"
TARGET:=$(shell ls *.md | head -n 1 | sed 's/\.md//g')
TARGETS=$(TARGET).html $(TARGET).pdf index.html

all: $(TARGETS)

clean:
	$(RM) $(TARGETS)

%.html: %.md
	$(PD) $< -o $@

%.pdf: %.md
	$(PD) $< -o $@

index.html: $(TARGET).html
	ln -s $< $@

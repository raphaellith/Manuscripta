export const stripLinksFromHtml = (html: string): string => {
    if (!html) return '';
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');

    Array.from(doc.querySelectorAll('a')).forEach((link) => {
        const parent = link.parentNode;
        if (!parent) {
            link.remove();
            return;
        }
        while (link.firstChild) {
            parent.insertBefore(link.firstChild, link);
        }
        parent.removeChild(link);
    });

    return doc.body.innerHTML;
};

import HtmlDiff from 'htmldiff-js';

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'dotDiff'
})
export class DotDiffPipe implements PipeTransform {
    transform(oldValue: string, newValue: string, showDiff = true): string {
        newValue = newValue || '';

        return showDiff ? HtmlDiff.execute(oldValue ? oldValue : '', newValue) : newValue;
    }
}

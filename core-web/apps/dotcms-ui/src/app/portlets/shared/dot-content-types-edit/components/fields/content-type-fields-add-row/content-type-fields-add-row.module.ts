import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { ContentTypeFieldsAddRowComponent } from './content-type-fields-add-row.component';

@NgModule({
    declarations: [ContentTypeFieldsAddRowComponent],
    exports: [ContentTypeFieldsAddRowComponent],
    imports: [
        CommonModule,
        ButtonModule,
        TooltipModule,
        UiDotIconButtonModule,
        SplitButtonModule,
        DotPipesModule
    ]
})
export class ContentTypeFieldsAddRowModule {}

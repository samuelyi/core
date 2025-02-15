/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFormResolver, DotUnlicensedPortlet } from './dot-form-resolver.service';

const route: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);

const messageServiceMock = new MockDotMessageService({
    'Forms-and-Form-Builder': 'Form',
    'Forms-and-Form-Builder-in-Enterprise': 'This a description',
    'Learn-more-about-dotCMS-Enterprise': 'Link 1',
    'Contact-Us-for-more-Information': 'Link 2'
});

describe('DotFormResolver', () => {
    let dotLicenseService: DotLicenseService;
    let service: DotFormResolver;

    beforeEach(waitForAsync(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotFormResolver,
                DotLicenseService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: []
        });

        dotLicenseService = testbed.get(DotLicenseService);
        service = testbed.get(DotFormResolver);
    }));

    it('should return resolve null when license is enterprise', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
        service.resolve(route).subscribe((result: DotUnlicensedPortlet) => {
            expect(result).toBe(null);
        });
    });

    it('should return resolve data when no license', () => {
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));
        service.resolve(route).subscribe((result: DotUnlicensedPortlet) => {
            expect(result).toEqual({
                title: 'Form',
                description: 'This a description',
                links: [
                    { text: 'Link 1', link: 'https://dotcms.com/product/features/feature-list' },
                    { text: 'Link 2', link: 'https://dotcms.com/contact-us/' }
                ]
            });
        });
    });
});

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router';
import { register } from '@/services/user/api/auth';
import { ApiError } from '@/common/api/ApiError';
import { ProblemType } from '@/common/api/ProblemType';
import { appendFromParam } from '@/common/lib/authRedirect';
import { Button } from '@/common/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/common/components/ui/card';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/common/components/ui/field';
import { Input } from '@/common/components/ui/input';
import { Spinner } from '@/common/components/ui/spinner';
import { Separator } from '@/common/components/ui/separator';

interface RegisterFormState {
  username: string;
  email: string;
  displayName: string;
  password: string;
  confirmPassword: string;
}

type RegisterFormErrors = Partial<Record<keyof RegisterFormState, string>>;

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+$/;

const EMPTY_FORM: RegisterFormState = {
  username: '',
  email: '',
  displayName: '',
  password: '',
  confirmPassword: '',
};

function validateEmail(email: string) {
  if (email.trim().length === 0)
    return 'auth.register.errors.emailRequired'
  else if (!EMAIL_PATTERN.test(email))
    return 'auth.register.errors.emailInvalid'
  else
    return undefined
}

function RegisterPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const from = searchParams.get('from');

  const [form, setForm] = useState<RegisterFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const [formErrors, setFormErrors] = useState<RegisterFormErrors>({});

  function updateField<K extends keyof RegisterFormState>(field: K, value: RegisterFormState[K]) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setFormErrors((errors) => ({...errors, [field]: undefined}));
  }

  function validate() {
    return {
      username:
        form.username.trim().length === 0
          ? 'auth.register.errors.usernameRequired'
          : undefined,
      email:
        validateEmail(form.email),
      displayName:
        form.displayName.trim().length === 0
          ? 'auth.register.errors.displayNameRequired'
          : undefined,
      password:
        form.password.length < 8
          ? 'auth.register.errors.weakPassword'
          : undefined,
      confirmPassword:
        form.password !== form.confirmPassword
          ? 'auth.register.errors.passwordMismatch'
          : undefined,
    };
  }

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setFormErrors({});

    const errors = validate();
    const isValid = Object.values(errors).every(error => error === undefined);

    if (!isValid) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    try {
      await register({
        username: form.username,
        email: form.email,
        displayName: form.displayName,
        password: form.password,
      });
      setIsSuccess(true);
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.WEAK_PASSWORD) {
        setFormErrors((previous) => ({ ...previous, password: 'auth.register.errors.weakPassword' }));
      } else if (error instanceof ApiError && error.type === ProblemType.USERNAME_ALREADY_EXISTS) {
        setFormErrors((previous) => ({ ...previous, username: 'auth.register.errors.usernameTaken' }));
      } else if (error instanceof ApiError && error.type === ProblemType.EMAIL_ALREADY_EXISTS) {
        setFormErrors((previous) => ({ ...previous, email: 'auth.register.errors.emailTaken' }));
      } else {
        setFormError(t('auth.register.errors.generic'));
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex justify-center px-6 py-12 sm:py-20">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>{t('auth.register.title')}</CardTitle>
          <CardDescription>{t('auth.register.description')}</CardDescription>
        </CardHeader>
        <CardContent>
          {isSuccess ? (
            <p className="text-sm text-foreground">
              {t('auth.register.success')}{' '}
              <Link to={appendFromParam('/login', from)} className="text-primary underline-offset-4 hover:underline">
                {t('auth.register.loginLink')}
              </Link>
            </p>
          ) : (
            <form onSubmit={handleSubmit} noValidate>
              <FieldGroup>
                <Field data-invalid={!!formErrors.username}>
                  <div className="flex items-baseline justify-between gap-2">
                    <FieldLabel htmlFor="register-username">
                      {t('auth.register.username')}
                      <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldError className="text-xs">
                      {formErrors.username && t(formErrors.username)}
                    </FieldError>
                  </div>
                  <Input
                    id="register-username"
                    autoComplete="username"
                    placeholder={t('auth.register.usernamePlaceholder')}
                    required
                    value={form.username}
                    onChange={(event) => updateField('username', event.target.value)}
                  />
                </Field>
                <Field data-invalid={!!formErrors.email}>
                  <div className="flex items-baseline justify-between gap-2">
                    <FieldLabel htmlFor="register-email">
                      {t('auth.register.email')}
                      <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldError className="text-xs">
                      {formErrors.email && t(formErrors.email)}
                    </FieldError>
                  </div>
                  <Input
                    id="register-email"
                    type="email"
                    autoComplete="email"
                    placeholder={t('auth.register.emailPlaceholder')}
                    required
                    value={form.email}
                    onChange={(event) => updateField('email', event.target.value)}
                  />
                </Field>
                <Field data-invalid={!!formErrors.displayName}>
                  <div className="flex items-baseline justify-between gap-2">
                    <FieldLabel htmlFor="register-display-name">
                      {t('auth.register.displayName')}
                      <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldError className="text-xs">
                      {formErrors.displayName && t(formErrors.displayName)}
                    </FieldError>
                  </div>
                  <Input
                    id="register-display-name"
                    autoComplete="nickname"
                    placeholder={t('auth.register.displayNamePlaceholder')}
                    required
                    value={form.displayName}
                    onChange={(event) => updateField('displayName', event.target.value)}
                  />
                </Field>
                <Field data-invalid={!!formErrors.password}>
                  <div className="flex items-baseline justify-between gap-2">
                    <FieldLabel htmlFor="register-password">
                      {t('auth.register.password')}
                      <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldError className="text-xs">
                      {formErrors.password && t(formErrors.password)}
                    </FieldError>
                  </div>
                  <Input
                    id="register-password"
                    type="password"
                    autoComplete="new-password"
                    required
                    minLength={8}
                    value={form.password}
                    onChange={(event) => updateField('password', event.target.value)}
                  />
                  <FieldDescription>{t('auth.register.passwordDescription')}</FieldDescription>
                </Field>
                <Field data-invalid={!!formErrors.confirmPassword}>
                  <div className="flex items-baseline justify-between gap-2">
                    <FieldLabel htmlFor="register-confirm-password">
                      {t('auth.register.confirmPassword')}
                      <span className="text-destructive">*</span>
                    </FieldLabel>
                    <FieldError className="text-xs">
                      {formErrors.confirmPassword && t(formErrors.confirmPassword)}
                    </FieldError>
                  </div>
                  <Input
                    id="register-confirm-password"
                    type="password"
                    autoComplete="new-password"
                    required
                    value={form.confirmPassword}
                    onChange={(event) => updateField('confirmPassword', event.target.value)}
                  />
                </Field>
                <div className="flex flex-col gap-2">
                  <div className="min-h-4">
                    <FieldError className="text-xs">{formError}</FieldError>
                  </div>
                  <Button type="submit" disabled={isSubmitting}>
                    {isSubmitting && <Spinner />}
                    {isSubmitting ? t('auth.register.submitting') : t('auth.register.submit')}
                  </Button>
                </div>
              </FieldGroup>
            </form>
          )}
        </CardContent>
        {!isSuccess && (
          <>
            <Separator/>
            <CardFooter className="justify-center text-sm text-muted-foreground">
              {t('auth.register.haveAccount')}{' '}
              <Link to={appendFromParam('/login', from)} className="ml-1 text-primary underline-offset-4 hover:underline">
                {t('auth.register.loginLink')}
              </Link>
            </CardFooter>
          </>
        )}
      </Card>
    </div>
  );
}

export default RegisterPage;

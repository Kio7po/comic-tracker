import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '@/common/components/AuthProvider';
import { ApiError } from '@/common/api/ApiError';
import { ProblemType } from '@/common/api/ProblemType';
import { Button } from '@/common/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/common/components/ui/card';
import { Checkbox } from '@/common/components/ui/checkbox';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/common/components/ui/field';
import { Input } from '@/common/components/ui/input';
import { Spinner } from '@/common/components/ui/spinner';
import { Separator } from '@/common/components/ui/separator';

interface LoginFormState {
  usernameOrEmail: string;
  password: string;
  rememberMe: boolean;
}

type LoginFormErrors = Partial<Record<keyof LoginFormState, string>>;

const EMPTY_FORM: LoginFormState = {
  usernameOrEmail: '',
  password: '',
  rememberMe: false,
};

function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState<LoginFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [formErrors, setFormErrors] = useState<LoginFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField<K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setFormErrors((errors) => ({ ...errors, [field]: undefined }));
  }

  function validate() {
    return {
      usernameOrEmail:
        form.usernameOrEmail.trim().length === 0
          ? 'auth.login.errors.usernameOrEmailRequired'
          : undefined,
      password: form.password.length === 0 ? 'auth.login.errors.passwordRequired' : undefined,
    };
  }

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setFormErrors({});

    const errors = validate();
    const isValid = Object.values(errors).every((error) => error === undefined);

    if (!isValid) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    try {
      await login({
        usernameOrEmail: form.usernameOrEmail,
        password: form.password,
        rememberMe: form.rememberMe,
      });
      navigate('/');
    } catch (error) {
      if (error instanceof ApiError && error.type === ProblemType.INVALID_CREDENTIALS) {
        setFormError(t('auth.login.errors.invalidCredentials'));
      } else {
        setFormError(t('auth.login.errors.generic'));
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex justify-center px-6 py-12 sm:py-20">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>{t('auth.login.title')}</CardTitle>
          <CardDescription>{t('auth.login.description')}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} noValidate>
            <FieldGroup>
              <Field data-invalid={!!formErrors.usernameOrEmail}>
                <div className="flex items-baseline justify-between gap-2">
                  <FieldLabel htmlFor="login-username-or-email">
                    {t('auth.login.usernameOrEmail')}
                    <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldError className="text-xs">
                    {formErrors.usernameOrEmail && t(formErrors.usernameOrEmail)}
                  </FieldError>
                </div>
                <Input
                  id="login-username-or-email"
                  autoComplete="username"
                  placeholder={t('auth.login.usernameOrEmailPlaceholder')}
                  required
                  value={form.usernameOrEmail}
                  onChange={(event) => updateField('usernameOrEmail', event.target.value)}
                />
              </Field>
              <Field data-invalid={!!formErrors.password}>
                <div className="flex items-baseline justify-between gap-2">
                  <FieldLabel htmlFor="login-password">
                    {t('auth.login.password')}
                    <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldError className="text-xs">
                    {formErrors.password && t(formErrors.password)}
                  </FieldError>
                </div>
                <Input
                  id="login-password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={form.password}
                  onChange={(event) => updateField('password', event.target.value)}
                />
              </Field>
              <Field orientation="horizontal">
                <Checkbox
                  id="login-remember-me"
                  checked={form.rememberMe}
                  onCheckedChange={(checked) => updateField('rememberMe', checked)}
                />
                <FieldLabel htmlFor="login-remember-me">{t('auth.login.rememberMe')}</FieldLabel>
              </Field>
              <div className="flex flex-col gap-2">
                <div className="min-h-4">
                  <FieldError className="text-xs">{formError}</FieldError>
                </div>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting && <Spinner />}
                  {isSubmitting ? t('auth.login.submitting') : t('auth.login.submit')}
                </Button>
                <Button type="button" variant="link" disabled className="mx-auto h-auto p-0 text-sm">
                  {t('auth.login.forgotPassword')}
                </Button>
              </div>
            </FieldGroup>
          </form>
        </CardContent>
        <Separator/>
        <CardFooter className="justify-center text-sm text-muted-foreground">
          {t('auth.login.noAccount')}{' '}
          <Link to="/register" className="ml-1 text-primary underline-offset-4 hover:underline">
            {t('auth.login.registerLink')}
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
}

export default LoginPage;

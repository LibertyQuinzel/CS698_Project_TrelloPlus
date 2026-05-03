import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router';
import { CheckCircle2, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { toast } from 'sonner';
import { apiService, type SecurityQuestionsResponse } from '../services/api';

const strongPasswordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/;

export function ForgotPassword() {
  const [step, setStep] = useState<'email' | 'answers' | 'password' | 'done'>('email');
  const [email, setEmail] = useState('');
  const [questions, setQuestions] = useState<SecurityQuestionsResponse | null>(null);
  const [answer1, setAnswer1] = useState('');
  const [answer2, setAnswer2] = useState('');
  const [customAnswer, setCustomAnswer] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const loadQuestions = async (event: FormEvent) => {
    event.preventDefault();
    if (!email.trim()) {
      toast.error('Please enter your email address');
      return;
    }

    setIsLoading(true);
    try {
      const response = await apiService.getSecurityQuestions(email.trim());
      setQuestions(response);
      setStep('answers');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to load recovery questions');
    } finally {
      setIsLoading(false);
    }
  };

  const validateAnswers = async (event: FormEvent) => {
    event.preventDefault();
    if (!answer1.trim() || !answer2.trim() || !customAnswer.trim()) {
      toast.error('Please answer all three questions');
      return;
    }

    setIsLoading(true);
    try {
      const response = await apiService.validateSecurityAnswers({
        email: email.trim(),
        answers: { answer1, answer2, customAnswer },
      });
      setResetToken(response.resetToken);
      toast.success(response.message);
      setStep('password');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Security answers did not match');
    } finally {
      setIsLoading(false);
    }
  };

  const resetPassword = async (event: FormEvent) => {
    event.preventDefault();
    if (newPassword !== confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    if (newPassword.length < 8 || !strongPasswordRegex.test(newPassword)) {
      toast.error('Password must include uppercase, lowercase, number, and special character');
      return;
    }

    setIsLoading(true);
    try {
      await apiService.resetPassword({ resetToken, newPassword });
      toast.success('Password reset successfully');
      setStep('done');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Password reset failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-3 mb-6">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center shadow-lg">
              <span className="text-white font-bold text-lg">AI</span>
            </div>
            <h1 className="text-3xl font-bold text-gray-900">FlowBoard</h1>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Reset your password</h2>
          <p className="text-gray-600">Use your recovery questions to regain access.</p>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-8">
          {step === 'email' && (
            <form onSubmit={loadQuestions} className="space-y-5">
              <div>
                <Label htmlFor="forgot-email">Email</Label>
                <Input
                  id="forgot-email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="mt-2"
                  disabled={isLoading}
                />
              </div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Continue'}
              </Button>
            </form>
          )}

          {step === 'answers' && questions && (
            <form onSubmit={validateAnswers} className="space-y-5">
              <div>
                <Label htmlFor="answer-1">{questions.question1}</Label>
                <Input id="answer-1" value={answer1} onChange={(event) => setAnswer1(event.target.value)} className="mt-2" disabled={isLoading} />
              </div>
              <div>
                <Label htmlFor="answer-2">{questions.question2}</Label>
                <Input id="answer-2" value={answer2} onChange={(event) => setAnswer2(event.target.value)} className="mt-2" disabled={isLoading} />
              </div>
              <div>
                <Label htmlFor="custom-answer">{questions.customQuestion}</Label>
                <Input id="custom-answer" value={customAnswer} onChange={(event) => setCustomAnswer(event.target.value)} className="mt-2" disabled={isLoading} />
              </div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Verify Answers'}
              </Button>
            </form>
          )}

          {step === 'password' && (
            <form onSubmit={resetPassword} className="space-y-5">
              <div>
                <Label htmlFor="new-password">New Password</Label>
                <Input id="new-password" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} className="mt-2" disabled={isLoading} />
                <p className="text-xs text-gray-500 mt-1">Use 8+ characters with uppercase, lowercase, number, and special character.</p>
              </div>
              <div>
                <Label htmlFor="confirm-new-password">Confirm New Password</Label>
                <Input id="confirm-new-password" type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} className="mt-2" disabled={isLoading} />
              </div>
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Reset Password'}
              </Button>
            </form>
          )}

          {step === 'done' && (
            <div className="text-center space-y-5">
              <CheckCircle2 className="w-12 h-12 text-green-600 mx-auto" />
              <p className="text-gray-700">Your password has been reset.</p>
              <Button asChild className="w-full">
                <Link to="/login">Return to Sign In</Link>
              </Button>
            </div>
          )}

          {step !== 'done' && (
            <div className="mt-6 text-center">
              <Link to="/login" className="text-sm text-blue-600 hover:text-blue-700 font-medium">
                Back to sign in
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

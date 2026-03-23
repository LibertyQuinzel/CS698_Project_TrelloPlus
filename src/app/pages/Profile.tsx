import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import { toast } from 'sonner';
import { apiService } from '../services/api';
import { useProjectStore } from '../store/projectStore';

export function Profile() {
  const navigate = useNavigate();
  const updateUser = useProjectStore((s) => s.updateUser);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const profile = await apiService.getUserProfile();
        setFullName(profile.fullName || profile.full_name || '');
        setEmail(profile.email || '');
      } catch (error) {
        toast.error('Failed to load profile');
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, []);

  const handleSave = async () => {
    if (!fullName.trim() || !email.trim()) {
      toast.error('Please fill in all fields');
      return;
    }

    setIsSaving(true);
    try {
      const updatedProfile = await apiService.updateUserProfile({ fullName, email });
      const resolvedName = updatedProfile.fullName || updatedProfile.full_name || fullName;
      const resolvedEmail = updatedProfile.email || email;

      const storedUserRaw = localStorage.getItem('user');
      const storedUser = storedUserRaw ? JSON.parse(storedUserRaw) : {};
      const mergedUser = {
        ...storedUser,
        ...updatedProfile,
        fullName: resolvedName,
        email: resolvedEmail,
      };
      localStorage.setItem('user', JSON.stringify(mergedUser));

      updateUser({
        name: resolvedName,
        email: resolvedEmail,
      });

      setFullName(resolvedName);
      setEmail(resolvedEmail);
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error((error as Error).message || 'Failed to update profile');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="p-4 md:p-8 pt-20 md:pt-24 min-h-screen">
        <div className="max-w-2xl mx-auto">
          <div className="flex flex-col items-center justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-4" />
            <p className="text-gray-600">Loading profile...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8 pt-20 md:pt-24 min-h-screen">
      <div className="max-w-2xl mx-auto">
        <Button variant="ghost" className="mb-4 -ml-2" onClick={() => navigate('/')}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Projects
        </Button>

        <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">Profile</h1>
        <p className="text-gray-600 mb-8">Manage your personal information</p>

        <div className="bg-white rounded-xl border border-gray-200 p-6 md:p-8 space-y-6">
          {/* Avatar */}
          <div className="flex items-center gap-4">
            <div className="relative">
              <Avatar className="w-20 h-20">
                <AvatarFallback className="text-xl">{fullName?.split(' ').map(n => n[0]).join('') || 'U'}</AvatarFallback>
              </Avatar>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">{fullName || 'User'}</h3>
              <p className="text-sm text-gray-500">{email}</p>
            </div>
          </div>

          <div className="border-t pt-6 space-y-4">
            <div>
              <Label htmlFor="profile-name">Full Name</Label>
              <Input
                id="profile-name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="mt-2"
              />
            </div>
            <div>
              <Label htmlFor="profile-email">Email</Label>
              <Input
                id="profile-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-2"
              />
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <Button variant="outline" onClick={() => navigate('/')} className="flex-1">Cancel</Button>
            <Button onClick={handleSave} disabled={isSaving} className="flex-1">
              {isSaving ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                'Save Changes'
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}